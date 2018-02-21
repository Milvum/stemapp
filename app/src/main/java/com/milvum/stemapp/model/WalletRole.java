package com.milvum.stemapp.model;

/**
 * .
 */

public enum WalletRole {
    PASS("pass"),
    BALLOT("ballot");

    private String name;

    WalletRole(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
