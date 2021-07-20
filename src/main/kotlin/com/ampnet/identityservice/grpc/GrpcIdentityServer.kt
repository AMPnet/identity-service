package com.ampnet.identityservice.grpc

import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.model.UserInfo
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.proto.GetUsersRequest
import com.ampnet.identityservice.proto.IdentityServiceGrpc
import com.ampnet.identityservice.proto.UserResponse
import com.ampnet.identityservice.proto.UsersResponse
import io.grpc.stub.StreamObserver
import mu.KLogging

class GrpcIdentityServer(
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository
) : IdentityServiceGrpc.IdentityServiceImplBase() {

    companion object : KLogging()

    override fun getUsers(request: GetUsersRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug { "Received gRPC getUsers: $request" }
        val users = userRepository.findAllById(request.addressesList)
        val usersInfos = userInfoRepository.findAllById(users.mapNotNull { it.userInfoUuid })
            .associateBy { it.uuid }
        val usersResponse = users.map { generateUserResponse(it, usersInfos[it.userInfoUuid]) }
        logger.debug { "UsersResponse size: ${usersResponse.size}" }
        val response = UsersResponse.newBuilder()
            .addAllUsers(usersResponse)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    fun generateUserResponse(user: User, userInfo: UserInfo?): UserResponse =
        UserResponse.newBuilder().apply {
            address = user.address
            email = user.email
            createdAt = user.createdAt.toEpochSecond()
            language = user.language ?: ""
            userInfo?.let {
                firstName = userInfo.firstName
                lastName = userInfo.lastName
                dateOfBirth = userInfo.dateOfBirth ?: ""
                personalNumber = userInfo.idNumber ?: ""
                documentNumber = userInfo.document.number ?: ""
            }
        }.build()
}
