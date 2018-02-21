package com.milvum.stemapp.utils;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.milvum.stemapp.R;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteItem;
import com.milvum.stemapp.model.VoteStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utils for retrieving VoteItems from assets
 */

public class VoteItemUtils {
    private static VoteCategory mainItem;

    /**
     * Get the first VoteItem of election.json. This is typically a VoteCategory (though no guarantees).
     * @param context - The context of the app, needed for file access
     * @return - VoteItem which is the parent of all other voteItems in the JSON file
     */
    public static VoteCategory getMainVoteItem(Context context) {
        if(mainItem != null) {
            return mainItem;
        }

        try {
            JSONObject jsonObject = new JSONObject(Utils.loadJSONFromAsset(context, "election.json"));
            mainItem = (VoteCategory) parseVoteItem(jsonObject);
        } catch (IOException | JSONException e) {
            Log.wtf("VoteItem", e);
            mainItem = new VoteCategory("Verkiezing", new ArrayList<>());
        }

        return mainItem;
    }

    /**
     * Returns a list of random VoteItems. Duplicates are possible.
     * @param context - Context required to get the MainVoteItem
     * @param amount - The amount of random VoteItems that should be returned
     * @return
     */
    public static List<VoteCandidate> getRandomVoteItems(Context context, int amount) {
        List<VoteCandidate> items = VoteItemUtils.getMainVoteItem(context).flatList();
        List<VoteCandidate> randomItems = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < amount; i++) {
            randomItems.add(items.get(random.nextInt(items.size())));
        }

        return randomItems;
    }

    /**
     * Fills a view with information on a VoteItem. Typically used in combination with vote_info_component
     * @param context - Context required to get MainVoteItem
     * @param view - The view that contains the TextFields that will be set.
     * @param indices - List of indices needed to find the targetted VoteItem through the Main VoteItem
     */
    public static void setViewContent(Context context, View view, ArrayList<Integer> indices) {
        VoteCategory category = (VoteCategory) VoteItemUtils.getMainVoteItem(context)
                .getDeepItem(indices.subList(0, indices.size() - 1));
        VoteCandidate candidate = (VoteCandidate) category.getDeepItem(indices.subList(indices.size() - 1, indices.size()));

        setViewContent(view,category.getName(), candidate.getName());
    }

    /**
     * Fills a view with information on a VoteItem. Typically used in combination with vote_info_component
     * @param view - The view that contains the TextFields that will be set.
     * @param breadCrumb - A string containing a breadcrumb-like title to display.
     * @param name - Name of the candidate to display.
     */
    public static void setViewContent(View view, String breadCrumb, String name) {
        Utils.setTextViewText(view, R.id.party_name, breadCrumb);
        Utils.setTextViewText(view, R.id.candidate_second_row, name);
    }

    private static VoteItem parseVoteItem(JSONObject object) throws JSONException {
        if(VoteCandidate.isVoteCandidate(object)) {
            return parseVoteCandidate(object);
        } else if (VoteCategory.isVoteCategory(object)) {
            return parseVoteCategory(object);
        } else {
            throw new RuntimeException("JSON is not parseable: " + object.toString());
        }
    }

    private static VoteCategory parseVoteCategory(JSONObject object) throws JSONException {
        JSONArray jsonArray = object.getJSONArray("items");
        List<VoteItem> items = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            items.add(parseVoteItem(jsonArray.getJSONObject(i)));
        }

        return new VoteCategory(
                object.getString("name"),
                items);
    }

    private static VoteCandidate parseVoteCandidate(JSONObject object) throws JSONException {
        return new VoteCandidate(
                object.getString("name"),
                object.getString("address"));
    }

}
