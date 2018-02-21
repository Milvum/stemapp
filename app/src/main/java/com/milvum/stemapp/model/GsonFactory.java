package com.milvum.stemapp.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

/**
 * Factory for obtaining a Gson instance. Use this factory if you want to get TypeAdapters included
 * such as for the VoteItem class.
 */
public class GsonFactory {
    private static final RuntimeTypeAdapterFactory<VoteItem> voteItemAdapter = RuntimeTypeAdapterFactory
            .of(VoteItem.class)
            .registerSubtype(VoteCandidate.class)
            .registerSubtype(VoteCategory.class);

    public static Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(voteItemAdapter)
                .create();
    }
}
