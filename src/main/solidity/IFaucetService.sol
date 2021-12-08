// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

interface IFaucetService {
    function faucet(address payable[] calldata _wallets) external;
    function updateRewardAmount(uint256 _newRewardAmount) external;
    function updateBalanceThresholdForReward(uint256 _newBalanceThresholdForReward) external;
    function updateCallerStatus(address _caller, bool _approved) external;
    function transferOwnership(address _newOwner) external;
    receive() external payable;
    function release() external;
}
