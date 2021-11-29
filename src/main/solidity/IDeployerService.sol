// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Structs.sol";

interface IDeployerService {

    struct InvestmentRecord {
        address investor;
        address campaign;
        uint256 amount;
    }

    struct InvestmentRecordStatus {
        address investor;
        address campaign;
        bool readyToInvest;
    }

    function getStatus(InvestmentRecord[] _investments) public view returns (InvestmentRecordStatus[] memory);

    function investFor(InvestmentRecord[] _investments) public;
}
