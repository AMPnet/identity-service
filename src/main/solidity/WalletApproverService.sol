// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./IVersioned.sol";
import "./Structs.sol";
import "./IIssuerCommon.sol";

interface WalletApproverService is IVersioned {
    function approveWallets(IIssuerCommon issuer, address payable [] memory wallets) external;
}
