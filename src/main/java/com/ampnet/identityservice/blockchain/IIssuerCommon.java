package com.ampnet.identityservice.blockchain;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.Arrays;

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
public class IIssuerCommon extends Contract {
    public static final String BINARY = "";

    public static final String FUNC_COMMONSTATE = "commonState";

    public static final String FUNC_FLAVOR = "flavor";

    public static final String FUNC_ISWALLETAPPROVED = "isWalletApproved";

    public static final String FUNC_VERSION = "version";

    @Deprecated
    protected IIssuerCommon(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IIssuerCommon(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IIssuerCommon(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IIssuerCommon(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<IssuerCommonState> commonState() {
        final Function function = new Function(FUNC_COMMONSTATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<IssuerCommonState>() {}));
        return executeRemoteCallSingleValueReturn(function, IssuerCommonState.class);
    }

    public RemoteFunctionCall<String> flavor() {
        final Function function = new Function(FUNC_FLAVOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isWalletApproved(String wallet) {
        final Function function = new Function(FUNC_ISWALLETAPPROVED, 
                Arrays.<Type>asList(new Address(160, wallet)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> version() {
        final Function function = new Function(FUNC_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static IIssuerCommon load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new IIssuerCommon(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IIssuerCommon load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new IIssuerCommon(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IIssuerCommon load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new IIssuerCommon(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IIssuerCommon load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IIssuerCommon(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<IIssuerCommon> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuerCommon.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IIssuerCommon> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IIssuerCommon.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<IIssuerCommon> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuerCommon.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<IIssuerCommon> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IIssuerCommon.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class IssuerCommonState extends DynamicStruct {
        public String flavor;

        public String version;

        public String contractAddress;

        public String owner;

        public String stablecoin;

        public String walletApprover;

        public String info;

        public IssuerCommonState(String flavor, String version, String contractAddress, String owner, String stablecoin, String walletApprover, String info) {
            super(new Utf8String(flavor),new Utf8String(version),new Address(contractAddress),new Address(owner),new Address(stablecoin),new Address(walletApprover),new Utf8String(info));
            this.flavor = flavor;
            this.version = version;
            this.contractAddress = contractAddress;
            this.owner = owner;
            this.stablecoin = stablecoin;
            this.walletApprover = walletApprover;
            this.info = info;
        }

        public IssuerCommonState(Utf8String flavor, Utf8String version, Address contractAddress, Address owner, Address stablecoin, Address walletApprover, Utf8String info) {
            super(flavor,version,contractAddress,owner,stablecoin,walletApprover,info);
            this.flavor = flavor.getValue();
            this.version = version.getValue();
            this.contractAddress = contractAddress.getValue();
            this.owner = owner.getValue();
            this.stablecoin = stablecoin.getValue();
            this.walletApprover = walletApprover.getValue();
            this.info = info.getValue();
        }
    }
}
