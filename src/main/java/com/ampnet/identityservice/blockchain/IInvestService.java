package com.ampnet.identityservice.blockchain;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.8.7.
 */
@SuppressWarnings("rawtypes")
public class IInvestService extends Contract {
    public static final String BINARY = "";

    public static final String FUNC_GETPENDINGFOR = "getPendingFor";

    public static final String FUNC_GETSTATUS = "getStatus";

    public static final String FUNC_INVESTFOR = "investFor";

    @Deprecated
    protected IInvestService(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IInvestService(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IInvestService(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IInvestService(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<List> getPendingFor(String _user, String _issuer, List<String> _campaignFactories, String queryService, String nameRegistry) {
        final Function function = new Function(FUNC_GETPENDINGFOR,
                Arrays.<Type>asList(new Address(160, _user),
                new Address(160, _issuer),
                new DynamicArray<Address>(
                        Address.class,
                        org.web3j.abi.Utils.typeMap(_campaignFactories, Address.class)),
                new Address(160, queryService),
                new Address(160, nameRegistry)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<PendingInvestmentRecord>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<List> getStatus(List<InvestmentRecord> _investments) {
        final Function function = new Function(FUNC_GETSTATUS,
                Arrays.<Type>asList(new DynamicArray<InvestmentRecord>(InvestmentRecord.class, _investments)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<InvestmentRecordStatus>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> investFor(List<InvestmentRecord> _investments) {
        final Function function = new Function(
                FUNC_INVESTFOR,
                Arrays.<Type>asList(new DynamicArray<InvestmentRecord>(InvestmentRecord.class, _investments)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static IInvestService load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new IInvestService(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IInvestService load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new IInvestService(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IInvestService load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new IInvestService(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IInvestService load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IInvestService(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<IInvestService> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IInvestService.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IInvestService> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IInvestService.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<IInvestService> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IInvestService.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IInvestService> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IInvestService.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class PendingInvestmentRecord extends StaticStruct {
        public String investor;

        public String campaign;

        public BigInteger allowance;

        public BigInteger balance;

        public BigInteger alreadyInvest;

        public Boolean kycPassed;

        public PendingInvestmentRecord(String investor, String campaign, BigInteger allowance, BigInteger balance, BigInteger alreadyInvest, Boolean kycPassed) {
            super(new Address(investor),new Address(campaign),new Uint256(allowance),new Uint256(balance),new Uint256(alreadyInvest),new Bool(kycPassed));
            this.investor = investor;
            this.campaign = campaign;
            this.allowance = allowance;
            this.balance = balance;
            this.alreadyInvest = alreadyInvest;
            this.kycPassed = kycPassed;
        }

        public PendingInvestmentRecord(Address investor, Address campaign, Uint256 allowance, Uint256 balance, Uint256 alreadyInvest, Bool kycPassed) {
            super(investor,campaign,allowance,balance,alreadyInvest,kycPassed);
            this.investor = investor.getValue();
            this.campaign = campaign.getValue();
            this.allowance = allowance.getValue();
            this.balance = balance.getValue();
            this.alreadyInvest = alreadyInvest.getValue();
            this.kycPassed = kycPassed.getValue();
        }
    }

    public static class InvestmentRecord extends StaticStruct {
        public String investor;

        public String campaign;

        public BigInteger amount;

        public InvestmentRecord(String investor, String campaign, BigInteger amount) {
            super(new Address(investor),new Address(campaign),new Uint256(amount));
            this.investor = investor;
            this.campaign = campaign;
            this.amount = amount;
        }

        public InvestmentRecord(Address investor, Address campaign, Uint256 amount) {
            super(investor,campaign,amount);
            this.investor = investor.getValue();
            this.campaign = campaign.getValue();
            this.amount = amount.getValue();
        }

        public InvestmentRecord(InvestmentRecordStatus recordStatus) {
            super(
                    new Address(recordStatus.investor),
                    new Address(recordStatus.campaign),
                    new Uint256(recordStatus.amount)
            );
            this.investor = recordStatus.investor;
            this.campaign = recordStatus.campaign;
            this.amount = recordStatus.amount;
        }
    }

    public static class InvestmentRecordStatus extends StaticStruct {
        public String investor;

        public String campaign;

        public BigInteger amount;

        public Boolean readyToInvest;

        public InvestmentRecordStatus(String investor, String campaign, BigInteger amount, Boolean readyToInvest) {
            super(new Address(investor),new Address(campaign),new Uint256(amount),new Bool(readyToInvest));
            this.investor = investor;
            this.campaign = campaign;
            this.amount = amount;
            this.readyToInvest = readyToInvest;
        }

        public InvestmentRecordStatus(Address investor, Address campaign, Uint256 amount, Bool readyToInvest) {
            super(investor,campaign,amount,readyToInvest);
            this.investor = investor.getValue();
            this.campaign = campaign.getValue();
            this.amount = amount.getValue();
            this.readyToInvest = readyToInvest.getValue();
        }
    }
}
