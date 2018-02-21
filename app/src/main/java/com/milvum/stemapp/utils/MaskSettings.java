package com.milvum.stemapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.milvum.stemapp.R;
import com.milvum.stemapp.model.GsonFactory;
import com.milvum.stemapp.model.VoteStorage;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Data class for managing settings for the masking job.
 */

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskSettings {
    private static final String MASKING_SETTINGS_KEY = "maskingSettings";

    private static final String TAG = "MaskSettings";

    @Getter // The masking votes that have been gathered so far
    private Queue<VoteStorage> maskingVotes;
    @Getter // The number of votes that still need to be retrieved.
    private int numberOfPostVotes;
    @Getter
    private int totalAmount;
    @Getter // The transaction hash of my (the real) vote
    private String myVoteHash;
    @Getter
    private BigInteger lastProcessedBlock;
    @Getter @Setter
    private boolean myVoteFound;

    /**
     * Constructor for creating a fresh Settings object
     * @param voteHash
     * @param numberOfVotes
     */
    private MaskSettings(String voteHash, int numberOfVotes, int totalAmount) {
        this.myVoteHash = voteHash;
        this.numberOfPostVotes = numberOfVotes;

        this.maskingVotes = new LinkedList<>();
        // TODO: Use a more constrained starting block
        // currently starts from first block. This will take too much time when the real chain is used.
        this.lastProcessedBlock = BigInteger.ZERO;
        this.myVoteFound = false;
        this.totalAmount = totalAmount;
    }

    /**
     * Add a masking vote to the collection. In addition, it is updated whether myVote has been
     * found, how many postVotes still need to be found and the collection is trimmed if needed
     * @param vote
     */
    public void addMaskVote(VoteStorage vote) {
        // Check if mainvote was found
        if(!myVoteFound && vote.txHash.equals(myVoteHash)) {
            myVoteFound = true;
            // increase number of post votes if not enough preVotes have been found
            numberOfPostVotes = Math.max(totalAmount - maskingVotes.size(), numberOfPostVotes);
        } else { // myVote will already have been stored in SharedPreferences so there is no need to include it
            maskingVotes.add(vote);
        }

        // The gathered masking votes might not fit into the range, if we exceed the amount of
        // required maskingVotes, the queue will have to be shifted
        if(maskingVotes.size() > totalAmount) {
            maskingVotes.remove();
        }

        // After myVote has been found, we can start counting down the number of postVotes
        if(myVoteFound) {
            numberOfPostVotes--;
        }
    }

    /**
     * The Masking process is considered done when both myVote has been found and the
     * right amount of postVotes has been found
     * @return Whether the gathering of MaskingVotes is done
     */
    public boolean isDone() {
        return myVoteFound && numberOfPostVotes < 0;
    }

    /**
     * Removes all previous settings in the SharedPreferences
     * @param context
     */
    public static void clear(Context context) {
        Log.d(TAG, "Clearing progress...");

        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(MASKING_SETTINGS_KEY);
        editor.commit();
    }

    /**
     * Retrieve a MaskSettings object from the SharedPreferences
     * @param context -
     * @return - The Settings object
     */
    public static MaskSettings get(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);

        Gson gson = GsonFactory.getGson();
        return gson.fromJson(preferences.getString(MASKING_SETTINGS_KEY, ""), MaskSettings.class);
    }

    /**
     * Stores the MaskSettings in the SharedPreferences
     * @param context -
     * @param settings -
     */
    public static void set(Context context, MaskSettings settings) {
        SharedPreferences preferences = context.getSharedPreferences(
            context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Gson gson = GsonFactory.getGson();
        editor.putString(MASKING_SETTINGS_KEY, gson.toJson(settings));

        editor.commit();
    }

    /**
     * Clear previous work and create settings for a new Masking Job
     * @param context - Required for accessing SharedPreferences
     * @param voteHash - The transactionhash of the vote which HAS to be included within the masking votes
     * @return - The newly created settings
     */
    public static MaskSettings init(Context context, String voteHash, int numberOfOptions) {
        // take 1 less than the number of options since myVote will be excluded
        int numberOfDummyvotes = numberOfOptions - 1;
        int numberOfPostVotes = new Random().nextInt(numberOfDummyvotes);

        MaskSettings settings = new MaskSettings(voteHash, numberOfPostVotes, numberOfDummyvotes);

        clear(context);
        set(context, settings);

        return settings;
    }
}
