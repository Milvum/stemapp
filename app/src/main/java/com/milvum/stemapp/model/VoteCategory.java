package com.milvum.stemapp.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A subset containing other voteItems. Used to open a nested CandidateListActivity
 */
public class VoteCategory extends VoteItem {
    private List<VoteItem> items;

    public VoteCategory(String name, List<VoteItem> items) {
        super(name);

        this.items = items;
    }

    public List<VoteItem> getItems() { return items; }

    @Override
    public VoteItem getDeepItem(List<Integer> indices) {
        if(indices.size() == 0) {
            return this;
        }

        VoteItem item = this.items.get(indices.get(0));
        List<Integer> otherIndices = indices.subList(1, indices.size());

        return item.getDeepItem(otherIndices);
    }

    @Override
    public List<VoteCandidate> flatList() {
        List<VoteCandidate> list = new ArrayList<>();
        for (VoteItem item : items) {
            list.addAll(item.flatList());
        }

        return list;
    }

    /**
     * Create a VoteStorage object according to supplied address and transaction hash.
     * This traverses the tree and stores the names of the path it has to take.
     * @param address - Wallet address of the candidate (usually a hex string without "0x"
     * @param txHash - Transaction hash to be stored in the VoteStorage object
     * @return -
     */
    public VoteStorage getVoteStorage(String address, String txHash) {
        VoteStorage voteStorage = null;
        for(int i = 0; i < items.size(); i++) {
            VoteItem item = items.get(i);
            if (item instanceof VoteCandidate) {
                if (address.equals(((VoteCandidate) item).getAddress())) {
                    voteStorage = new VoteStorage(new ArrayList<>(), item, txHash);
                    break;
                }
            } else if (item instanceof VoteCategory) {
                voteStorage = ((VoteCategory) item).getVoteStorage(address, txHash);
                if(voteStorage != null) {
                    break;
                }
            }
        }

        if(voteStorage != null) {
            voteStorage.addBreadCrumb(getName());
        }

        return voteStorage;
    }

    /**
     * Create a VoteStorage object for the item at the end of the supplied path.
     * This is a faster version of getVoteStorage(String, String) as there will not be a need to
     * find the correct path.
     * @param indices - List of indices that indicate the path through the tree
     * @param txHash - Transaction hash to be stored in the VoteStorage object
     * @return -
     */
    public VoteStorage getVoteStorage(List<Integer> indices, String txHash) {
        VoteStorage vs;
        VoteItem item = items.get(indices.get(0));
        if(indices.size() == 1) {
            vs = new VoteStorage(new ArrayList<>(), item, txHash);
        } else {
            vs = ((VoteCategory) item).getVoteStorage(indices.subList(1, indices.size()), txHash);
        }

        vs.addBreadCrumb(this.getName());
        return vs;
    }

    public static boolean isVoteCategory(JSONObject object) {
        return object.has("items");
    }
}
