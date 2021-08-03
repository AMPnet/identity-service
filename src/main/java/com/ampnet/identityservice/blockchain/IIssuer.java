package com.ampnet.identityservice.blockchain;

import java.util.Arrays;
import java.util.Collections;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.8.4.
 */
@SuppressWarnings("rawtypes")
public class IIssuer extends Contract {
    public static final String BINARY = "";

    public static final String FUNC_APPROVEWALLET = "approveWallet";

    public static final String FUNC_ISWALLETAPPROVED = "isWalletApproved";

    public static final String FUNC_SUSPENDWALLET = "suspendWallet";

    protected IIssuer(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected IIssuer(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> approveWallet(String wallet) {
        final Function function = new Function(
                FUNC_APPROVEWALLET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, wallet)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isWalletApproved(String _wallet) {
        final Function function = new Function(FUNC_ISWALLETAPPROVED,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _wallet)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> suspendWallet(String wallet) {
        final Function function = new Function(
                FUNC_SUSPENDWALLET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, wallet)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static IIssuer load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new IIssuer(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IIssuer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IIssuer(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<IIssuer> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuer.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<IIssuer> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuer.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }
}
