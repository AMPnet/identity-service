package com.ampnet.identityservice.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import mu.KLogging

class GrpcLogInterceptor : ClientInterceptor {

    companion object : KLogging()

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        logger.debug { method.fullMethodName }
        return next.newCall(method, callOptions)
    }
}
