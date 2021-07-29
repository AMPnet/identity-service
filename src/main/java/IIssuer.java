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
public class IIssuer extends org.web3j.tx.Contract {
    public static final String BINARY = "";

    public static final String FUNC_APPROVEWALLET = "approveWallet";

    public static final String FUNC_GETASSETS = "getAssets";

    public static final String FUNC_GETCFMANAGERS = "getCfManagers";

    public static final String FUNC_INFO = "info";

    public static final String FUNC_ISWALLETAPPROVED = "isWalletApproved";

    public static final String FUNC_STABLECOIN = "stablecoin";

    @Deprecated
    protected IIssuer(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IIssuer(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IIssuer(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IIssuer(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public org.web3j.protocol.core.RemoteFunctionCall<org.web3j.protocol.core.methods.response.TransactionReceipt> approveWallet(String _wallet) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_APPROVEWALLET, 
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(new org.web3j.abi.datatypes.Address(160, _wallet)),
                java.util.Collections.<org.web3j.abi.TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public org.web3j.protocol.core.RemoteFunctionCall<java.util.List> getAssets() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETASSETS,
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(),
                java.util.Arrays.<org.web3j.abi.TypeReference<?>>asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>>() {}));
        return new org.web3j.protocol.core.RemoteFunctionCall<java.util.List>(function,
                new java.util.concurrent.Callable<java.util.List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public java.util.List call() throws Exception {
                        java.util.List<org.web3j.abi.datatypes.Type> result = (java.util.List<org.web3j.abi.datatypes.Type>) executeCallSingleValueReturn(function, java.util.List.class);
                        return convertToNative(result);
                    }
                });
    }

    public org.web3j.protocol.core.RemoteFunctionCall<java.util.List> getCfManagers() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETCFMANAGERS,
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(),
                java.util.Arrays.<org.web3j.abi.TypeReference<?>>asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>>() {}));
        return new org.web3j.protocol.core.RemoteFunctionCall<java.util.List>(function,
                new java.util.concurrent.Callable<java.util.List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public java.util.List call() throws Exception {
                        java.util.List<org.web3j.abi.datatypes.Type> result = (java.util.List<org.web3j.abi.datatypes.Type>) executeCallSingleValueReturn(function, java.util.List.class);
                        return convertToNative(result);
                    }
                });
    }

    public org.web3j.protocol.core.RemoteFunctionCall<org.web3j.protocol.core.methods.response.TransactionReceipt> info() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INFO, 
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(),
                java.util.Collections.<org.web3j.abi.TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public org.web3j.protocol.core.RemoteFunctionCall<Boolean> isWalletApproved(String _wallet) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISWALLETAPPROVED,
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(new org.web3j.abi.datatypes.Address(160, _wallet)),
                java.util.Arrays.<org.web3j.abi.TypeReference<?>>asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public org.web3j.protocol.core.RemoteFunctionCall<String> stablecoin() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_STABLECOIN,
                java.util.Arrays.<org.web3j.abi.datatypes.Type>asList(),
                java.util.Arrays.<org.web3j.abi.TypeReference<?>>asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static IIssuer load(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        return new IIssuer(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IIssuer load(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        return new IIssuer(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IIssuer load(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        return new IIssuer(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IIssuer load(String contractAddress, org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        return new IIssuer(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static org.web3j.protocol.core.RemoteCall<IIssuer> deploy(org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuer.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static org.web3j.protocol.core.RemoteCall<IIssuer> deploy(org.web3j.protocol.Web3j web3j, org.web3j.crypto.Credentials credentials, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        return deployRemoteCall(IIssuer.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static org.web3j.protocol.core.RemoteCall<IIssuer> deploy(org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, org.web3j.tx.gas.ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IIssuer.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static org.web3j.protocol.core.RemoteCall<IIssuer> deploy(org.web3j.protocol.Web3j web3j, org.web3j.tx.TransactionManager transactionManager, java.math.BigInteger gasPrice, java.math.BigInteger gasLimit) {
        return deployRemoteCall(IIssuer.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
