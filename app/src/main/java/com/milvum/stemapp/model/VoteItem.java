package com.milvum.stemapp.model;

import java.util.List;

/**
 * Data class for elections.
 */

public abstract class VoteItem {
    private String name;

    VoteItem(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public abstract VoteItem getDeepItem(List<Integer> indices);
    public abstract List<VoteCandidate> flatList();
}
