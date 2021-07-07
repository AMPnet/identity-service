// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./IIssuer.sol";

contract Issuer is IIssuer {

    address public owner;
    address public override stablecoin;
    mapping (address => bool) public approvedWallets;
    address[] public assets;
    address[] public cfManagers;
    string public override info;

    modifier onlyOwner {
        require(msg.sender == owner);
        _;
    }

    modifier walletApproved(address _wallet) {
        require(
            approvedWallets[_wallet],
            "This action is forbidden. Wallet not approved by the Issuer."
        );
        _;
    }

    function approveWallet(address _wallet) external onlyOwner {
        approvedWallets[_wallet] = true;
    }

    function suspendWallet(address _wallet) external onlyOwner {
        approvedWallets[_wallet] = false;
    }

    function setInfo(string memory _info) external onlyOwner {
        info = _info;
    }

    function isWalletApproved(address _wallet) external view override returns (bool) {
        return approvedWallets[_wallet];
    }

    function getAssets() external override view returns (address[] memory) { return assets; }

    function getCfManagers() external override view returns (address[] memory) { return cfManagers; }
}
