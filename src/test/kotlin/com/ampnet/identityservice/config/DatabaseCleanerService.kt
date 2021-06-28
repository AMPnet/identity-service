package com.ampnet.identityservice.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllRefreshTokens() {
        em.createNativeQuery("DELETE FROM refresh_token").executeUpdate()
    }
}
