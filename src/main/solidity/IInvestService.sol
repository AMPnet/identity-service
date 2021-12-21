// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Structs.sol";

interface IInvestService {
    struct PendingInvestmentRecord {
        address investor;
        address campaign;
        uint256 allowance;
        uint256 balance;
        uint256 alreadyInvest;
        bool kycPassed;
    }

    struct InvestmentRecord {
        address investor;
        address campaign;
        uint256 amount;
    }

    struct InvestmentRecordStatus {
        address investor;
        address campaign;
        uint256 amount;
        bool readyToInvest;
    }

    function getPendingFor(
        address _user,
        address _issuer,
        address[] calldata _campaignFactories,
        address queryService,
        address nameRegistry
    ) external view returns (PendingInvestmentRecord[] memory);

    function getStatus(
        InvestmentRecord[] calldata _investments
    ) external view returns (InvestmentRecordStatus[] memory);

    function investFor(InvestmentRecord[] calldata _investments) external;
}
