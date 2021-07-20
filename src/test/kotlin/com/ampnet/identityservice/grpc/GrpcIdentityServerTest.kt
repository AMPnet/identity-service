package com.ampnet.identityservice.grpc

import com.ampnet.identityservice.TestBase
import com.ampnet.identityservice.persistence.model.User
import com.ampnet.identityservice.persistence.repository.UserInfoRepository
import com.ampnet.identityservice.persistence.repository.UserRepository
import com.ampnet.identityservice.proto.GetUsersRequest
import com.ampnet.identityservice.proto.UsersResponse
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class GrpcIdentityServerTest : TestBase() {

    private val userRepository = mock<UserRepository>()
    private val userInfoRepository = mock<UserInfoRepository>()

    private lateinit var grpcService: GrpcIdentityServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(userRepository)
        Mockito.reset(userInfoRepository)
        grpcService = GrpcIdentityServer(userRepository, userInfoRepository)
        testContext = TestContext()
    }

    @Test
    fun mustReturnRequestedUsers() {
        suppose("Users exist") {
            testContext.addresses = listOf("0x0", "0x01")
            testContext.users = createListOfUser(testContext.addresses)
            given(userRepository.findAllById(testContext.addresses)).willReturn(testContext.users)
        }

        verify("Grpc service will return users") {
            val request = GetUsersRequest.newBuilder()
                .addAllAddresses(testContext.addresses)
                .build()

            val streamObserver = mock<StreamObserver<UsersResponse>>()
            grpcService.getUsers(request, streamObserver)
            val usersResponse = testContext.users.map { grpcService.generateUserResponse(it, null) }
            val response = UsersResponse.newBuilder().addAllUsers(usersResponse).build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustReturnEmptyList() {
        verify("Grpc service will not fail on missing address") {
            val request = GetUsersRequest.newBuilder()
                .addAddresses("missing-address")
                .build()

            val streamObserver = mock<StreamObserver<UsersResponse>>()
            grpcService.getUsers(request, streamObserver)
            val response = UsersResponse.newBuilder().clearUsers().build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    private fun createListOfUser(addresses: List<String>): List<User> {
        val users = mutableListOf<User>()
        addresses.forEach {
            val user = createUser(it)
            users.add(user)
        }
        return users
    }

    private fun createUser(address: String): User =
        User(address, "email@mail.com", null, ZonedDateTime.now(), null)

    private class TestContext {
        lateinit var addresses: List<String>
        lateinit var users: List<User>
    }
}
