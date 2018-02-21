package com.milvum.stemapp.geth.implementation;

/**
 * .
 *
 * Contains the addresses from the deployed smart contracts.
 * It is typically read from a json file.
 */
public class ContractInfo {
    public String votingPassAddressHex;
    public String ballotDispenserAddressHex;
    public String votingBallotAddressHex;

    public ContractInfo(
            String votingPassAddressHex,
            String ballotDispenserAddressHex,
            String votingBallotAddressHex) {

        this.votingPassAddressHex = votingPassAddressHex;
        this.ballotDispenserAddressHex = ballotDispenserAddressHex;
        this.votingBallotAddressHex = votingBallotAddressHex;
    }
}