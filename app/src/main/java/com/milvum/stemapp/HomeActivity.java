package com.milvum.stemapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.milvum.stemapp.ballotexchange.ExchangeScheduler;
import com.milvum.stemapp.ballotexchange.HttpRedeemClient;
import com.milvum.stemapp.ballotexchange.MixJob;
import com.milvum.stemapp.ballotexchange.MixState;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.geth.implementation.contract.VotingPassUtil;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.ToastUtil;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.utils.WalletUtil;
import com.milvum.stemapp.view.DebouncedClickListener;

import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class HomeActivity extends AppCompatActivity
{
    private Dialog dialog = null;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (!pm.isIgnoringBatteryOptimizations(this.getPackageName()) && !BuildConfig.DEBUG)
        {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            this.startActivity(intent);
        }

        setContentView(R.layout.activity_home);

        final Wallet passWallet = WalletUtil.fromKey(this, WalletRole.PASS);
        handler = new Handler(getMainLooper());

        if (BuildConfig.BEG_ADDRESS != null)
        {
            HttpRedeemClient client = new HttpRedeemClient(this);
            client.beg(WalletUtil.fromKey(getApplicationContext(), WalletRole.PASS).getAddressHex());
            Log.d("Beg", "Hobo be begging");
        }

        dialog = Utils.createQRCodeDialog(this, passWallet.getAddressHex());

        Button nextButton = (Button) this.findViewById(R.id.nextButton);
        Button verifyButton = (Button) this.findViewById(R.id.verifyButton);
        ImageButton quitImageButton = (ImageButton) this.findViewById(R.id.quitImageButton);

        if (VoteUtil.getInstance(getApplicationContext()).getIdSet().isEmpty())
        {
            verifyButton.setVisibility(View.GONE);
            quitImageButton.setVisibility(View.GONE);
        }

        nextButton.setOnClickListener(onVoteClick);

        verifyButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        if (VoteUtil.getInstance(getApplicationContext()).getIdSet().size() <= 1)
                        {
                            ToastUtil.showToast(HomeActivity.this, R.string.errorNotEnoughMasking, Toast.LENGTH_LONG);
                            return;
                        }

                        Intent i = new Intent(HomeActivity.this, VotingTilesActivty.class);
                        startActivity(i);
                    }
                }
        );
    }

    public void closeDialog(View view)
    {
        this.dialog.dismiss();
    }

    public void showQRDialog(View view)
    {
        this.dialog.show();
    }

    public void quitDummy(View view)
    {
        // This implements a virtual click on the home button.
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /**
     * Debounced Click listener that will check the Ether and Pass balance of supplied passWallet.
     * Will stay on homescreen if requirements are not met (ether, votingpass and blockchain online).
     * Continues to new activity if mixing is already started or can start mix.
     */
    private View.OnClickListener onVoteClick = new DebouncedClickListener()
    {
        @Override
        protected void clickAction(View v)
        {
            Wallet passWallet = WalletUtil.fromKey(getApplicationContext(), WalletRole.PASS);
            Wallet ballotWallet = WalletUtil.fromKey(getApplicationContext(), WalletRole.BALLOT);

            new Thread(new CheckMixingStateRunnable(passWallet, ballotWallet)).start();
        }

        class CheckMixingStateRunnable implements Runnable
        {
            private final String TAG = "Mixing";
            private Wallet passWallet;
            private Wallet ballotWallet;

            CheckMixingStateRunnable(Wallet passWallet, Wallet ballotWallet)
            {
                this.passWallet = passWallet;
                this.ballotWallet = ballotWallet;
            }

            @Override
            public void run()
            {
                if (tryStartMix())
                {
                    handler.post(new StartActivityRunnable());
                }
                done();
            }

            /**
             * Check requirements for starting a mix and start it if possible.
             * @return - Indicate whether mix is running.
             */
            private boolean tryStartMix()
            {
                if (MixState.getMixState(HomeActivity.this) != MixState.IDLE)
                {
                    Log.d(TAG, "Not starting mix service, it is already running");
                    return true;
                }

                Activity activity = HomeActivity.this;
                Context context = getApplicationContext();

                // Request ether and pass balance
                BigInteger passBalance, ballotBalance, etherBalance;
                try
                {
                    passBalance = VotingPassUtil.getContract(context, passWallet)
                            .balanceOf(passWallet.getAddressHex()).send();
                    ballotBalance = VotingBallotUtil.getContract(context, ballotWallet)
                            .balanceOf(ballotWallet.getAddressHex()).send();
                    etherBalance = Web3jProvider.getWeb3j().ethGetBalance(
                            passWallet.getAddressHex(),
                            DefaultBlockParameterName.LATEST).send().getBalance();
                } catch (Exception e)
                {
                    Log.w(TAG, "Blockchain is offline", e);
                    ToastUtil.showToast(activity, R.string.errorNoBlockchain, Toast.LENGTH_SHORT);
                    return false;
                }

                if (ballotBalance.compareTo(BigInteger.ZERO) > 0)
                {
                    Log.d(TAG, "In posession of VotingBallots, not starting new mix");
                    return true;
                }

                // Check Ether balance
                if (etherBalance.compareTo(BigInteger.ZERO) != 1)
                {
                    Log.w(TAG, "Insufficient Ether, when attempting to start mix");
                    ToastUtil.showToast(activity, R.string.errorNotEnoughEther, Toast.LENGTH_SHORT);
                    return false;
                }

                // Check Pass balance
                if (passBalance.compareTo(BigInteger.ZERO) != 1)
                {
                    Log.w(TAG, "No voting pass when attempting to start mix");
                    ToastUtil.showToast(activity, R.string.errorNoVotingPass, Toast.LENGTH_SHORT);
                    return false;
                }

                Log.d("Mixing", "Register MixingJob");
                ExchangeScheduler.scheduleJob(context, MixJob.REQUEST);
                return true;
            }
        }

        class StartActivityRunnable implements Runnable
        {
            @Override
            public void run()
            {
                Intent i = new Intent(getApplicationContext(), CandidateListActivity.class);
                i.putIntegerArrayListExtra(Constants.VOTE_ITEMS, new ArrayList<Integer>());
                startActivity(i);
            }
        }
    };
}
