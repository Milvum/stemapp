# [StemApp](https://milvum.github.io/stemapp/)

At the end of 2016 we started our journey with the goal: making digital voting possible for The Netherlands. In co-creation with five municipalities, we worked together to enable citizen participation on the blockchain. We believe this is a perfect step for making digital voting on the blockchain a reality. We have worked hard to realize the project and are pleased that a first version is now available for further development. That is why we are now doing an Open Source release of our StemApp project. We invite developers and security researchers to help The Netherlands in the next phase of digital voting. We be

Please mention [Milvum](https://milvum.com) in communications when using this code.

# StemApp

The android app used to cast votes. Before this app works you need to have:
1. Deployed the [contracts](https://github.com/Milvum/contracts).
1. Have a [ballot-dispenser](https://github.com/Milvum/ballot-dispenser) up and running.

When these requirements are met, StemApp will require some configuration:
1. Ensure correct contract addresses in your build configuration (e.g. `app/src/main/assets/ContractInfo.debug.json`). The correct addresses can be found in the [contracts repo](https://github.com/Milvum/contracts).
1. Import the Abi's from the compiled contracts. Once again check [contracts](https://github.com/Milvum/contracts) on how to do this. Gradle will use these ABI's to build java classes of the contracts which are used throughout the app.
1. The candidate list, `app/src/main/assets/election.json`, needs to be up to date (can be copied from [candidates](https://github.com/candidates))
1. The endpoints and ChainID in `app/build.gradle` need to be up to date for your build-variant. These are currently aimed at `10.0.2.2` which is `localhost` from the perspective of an android simulator. ChainID is currently set at 9351, the default id for the private Ethereum node.

Once this is correctly set up, you should be able to run the app. The app will acquire its own Voting Pass if you use a build variant where `BEG_ADDRESS` is set. Otherwise the [Overseer](https://github.com/Milvum/overseer) will have to be used in conjunction to provide a Voting Pass for you.

## Disclaimer

The project in the current state is not market ready and thus should only be used for pilots or testing. In its current state the StemApp is not yet fully tested and not entirely secure (see open issues in the [whitepaper](https://milvum.com/en/download-stemapp-whitepaper/)). This version is also not yet ready for a release on the public Ethereum network. Milvum is not accountable for the use of the StemApp in any way, and the possible outcomes this may have.
