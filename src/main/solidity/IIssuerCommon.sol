// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./IVersioned.sol";
import "./Structs.sol";

interface IIssuerCommon is IVersioned {
    function isWalletApproved(address wallet) external view returns (bool);
    function commonState() external view returns (Structs.IssuerCommonState memory);
}
