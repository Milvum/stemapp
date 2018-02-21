package com.milvum.stemapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.milvum.stemapp.R;
import com.milvum.stemapp.model.GsonFactory;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.VoteCategory;
import com.milvum.stemapp.model.VoteItem;
import com.milvum.stemapp.model.VoteStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * .
 * Maintains a number of random voting transactions of which one is the vote of the user
 */

public class VoteUtil {
    private static final String KEYS_KEY = "voteKeys";
    private static VoteUtil instance = null;
    private Gson gson;

    private Map<String, VoteStorage> voteStorageLookup;

    private VoteUtil(Map<String, VoteStorage> voteStorageLookup) {
        this.voteStorageLookup = voteStorageLookup;
        
        this.gson = GsonFactory.getGson();
    }
    private List<Character> popList = null;

    /**
     * @return - A set of keys/ids to which the voting storage is linked
     */
    public Set<String> getIdSet() {
        return voteStorageLookup.keySet();
    }

    /**
     * Add a vote+transactionhash to the existing map and attaches a random key to it
     * @param voteStorage - the transaction hash
     * @return The generated key
     */
    public String addVote(VoteStorage voteStorage) {
        String key = getUniqueKey();
        voteStorageLookup.put(key, voteStorage);

        return key;
    }

    public VoteStorage getVote(Context context, String key) {
        if(!voteStorageLookup.containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " does not exist in voteStorage.");
        }

        return voteStorageLookup.get(key);
    }

    public void storeVotes(Context context) {
        Set<String> keys = voteStorageLookup.keySet();

        SharedPreferences.Editor prefEditor = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE).edit();

        prefEditor.putStringSet(KEYS_KEY, keys);
        for (String key : keys) {
            prefEditor.putString(key, gson.toJson(voteStorageLookup.get(key)));
        }
        prefEditor.apply();
    }

    private String getUniqueKey() {
        if (popList == null) {
            popList = new ArrayList<>(Constants.TOKENS.length());
            for (char c: Constants.TOKENS.toCharArray()){
                popList.add(c);
            }
            Collections.shuffle(popList);
        }

        if (popList.isEmpty()) {
            throw new IndexOutOfBoundsException("Trying to access a random token while no tokens are left.");
        }

        return String.valueOf(popList.remove(0));
    }

    public static VoteUtil getInstance(Context context) {
        if(instance == null) {
            instance = new VoteUtil(getMapFromPreferences(context));
        }

        return instance;
    }

    /**
     * Get a map of voting transaction info based on the stored prefences.
     * @param context
     * @return
     */
    private static Map<String, VoteStorage> getMapFromPreferences(Context context) {
        Map<String, VoteStorage> map = new HashMap<>();

        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);

        // get the stored keys
        Set<String> keys = preferences.getStringSet(KEYS_KEY, null);

        // if keys exist, find each linked value
        if(keys != null) {
            String jsonTemp;
            VoteStorage vs;
            for (String key : keys) {
                jsonTemp = preferences.getString(key, null);
                if (jsonTemp == null) {
                    Log.wtf("VoteUtils", "sharedPreference vote is not found under key " + key);
                }
                vs = (GsonFactory.getGson()).fromJson(jsonTemp, VoteStorage.class);
                map.put(key, vs);
            }
        }

        return map;
    }

    public static void clearVotes(Context context) {
        instance = null;

        // clear stored votes
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> keys = preferences.getStringSet(KEYS_KEY, null);
        if(keys == null) {
            return;
        }

        for (String key : keys) {
            editor.remove(key);
        }
        editor.remove(KEYS_KEY);

        editor.apply();
    }
}
