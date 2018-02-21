package com.milvum.stemapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.view.VoteAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VotingTilesActivty extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting_tiles);
        setTitle(getString(R.string.votingTilesTitle));

        final GridView voteGrid = (GridView) findViewById(R.id.vote_grid);

        VoteAdapter adapter = new VoteAdapter(this, R.layout.vote_square, getVoteList());
        voteGrid.setAdapter(adapter);

        voteGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(VotingTilesActivty.this, VerificationActivity.class);
                intent.putExtra(Constants.VOTE_BUNDLE, (String) voteGrid.getItemAtPosition(position));
                startActivity(intent);
            }
        });
    }

    private List<String> getVoteList() {
        Set<String> voteSet = VoteUtil.getInstance(getApplicationContext()).getIdSet();

        List<String> voteList = new ArrayList<>(voteSet);
        Collections.shuffle(voteList);

        return voteList;
    }
}
