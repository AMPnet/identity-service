// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract Structs {

    struct IssuerCommonState {
        string flavor;
        string version;
        address contractAddress;
        address owner;
        address stablecoin;
        address walletApprover;
        string info;
    }

}
