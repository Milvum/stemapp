package com.milvum.stemapp.utils;

/**
 * .
 */

public final class Constants
{

    /**
     * Restricts instantiation
     */
    private Constants()
    {
    }

    public static final int BEG_TIMEOUT_SECONDS = 25;
    public static final String PARTY_NAME = "Party_Name";
    public static final String VOTE_BUNDLE = "VoteBundle";
    public static final String CANDIDATE = "Candidate";
    public static final String CONFIRMATION_DIALOG = "Confirmation Dialog";
    public static final int SECRET_WAIT_TIME = 1000 * 1;
    public static final String TOKENS = "abcdefghijklmnopqrstuvwxyz";
    public static final String TOKEN_PREFS = "TokenPrefs";
    public static final String VOTE_PREFS = "VotePrefs";
    public static final String VOTE_ID = "votes";
    public static final int AMOUNT_VOTES = 10; // should ALWAYS be smaller than the length of Constants.TOKENS
    public static final int MAX_MASKING_TIME = 1000 * 60 * 5; // in miliseconds
    public static final int VOTE_ID_LIMIT = 100000;
    public static final String VOTE_ITEMS = "voteItems";

    public static final long LOAD_GAS_LIMIT = 2000000L;
    public static final long LOAD_GAS_PRICE = 40000000000L;
}
