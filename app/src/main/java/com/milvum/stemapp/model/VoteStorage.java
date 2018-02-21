package com.milvum.stemapp.model;

import android.app.PendingIntent;
import android.renderscript.RSInvalidStateException;

import com.milvum.stemapp.model.VoteItem;

import java.util.List;

/**
 * VoteStorage for storing a vote together with its corresponding transaction
 * hash in SharedPreferences.
 */

public class VoteStorage {
    public List<String> breadCrumbs;
    public VoteItem voteItem;
    public String txHash;

    public VoteStorage(List<String> breadCrumbs, VoteItem voteItem, String txHash) {
        this.breadCrumbs = breadCrumbs;
        this.voteItem = voteItem;
        this.txHash = txHash;
    }

    public String getLastBreadCrumb() {
        if (this.breadCrumbs.isEmpty()) {
            throw new IllegalStateException("BreadCrumbs should never be empty");
        }

        return this.breadCrumbs.get(this.breadCrumbs.size() -1);
    }

    public void addBreadCrumb(String name) {
        breadCrumbs.add(0, name);
    }
}
