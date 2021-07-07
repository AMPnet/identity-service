// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

interface IIssuer {
    function stablecoin() external view returns (address);
    function isWalletApproved(address _wallet) external view returns (bool);
    function info() external returns (string memory);
    function getAssets() external view returns (address[] memory);
    function getCfManagers() external view returns (address[] memory);
}
