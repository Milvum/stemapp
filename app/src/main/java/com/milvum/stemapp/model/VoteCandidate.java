package com.milvum.stemapp.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * An item which can actually be voted on.
 * Contains a ethereum address to which a Voting Ballot can be transferred
 */
public class VoteCandidate extends VoteItem {
    private String address;

    public VoteCandidate(String name, String address) {
        super(name);

        this.address = address;
    }

    public String getAddress() { return address; }

    @Override
    public VoteItem getDeepItem(List<Integer> indices) {
        if(indices.size() == 0) {
            return this;
        }

        throw new IllegalAccessError("Should not try to access getDeepItem for a VoteCandidate");
    }

    @Override
    public List<VoteCandidate> flatList() {
        List<VoteCandidate> list = new ArrayList<>();
        list.add(this);
        return list;
    }

    public static boolean isVoteCandidate(JSONObject object) {
        return object.has("address");
    }
}
