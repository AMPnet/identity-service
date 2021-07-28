package com.ampnet.identityservice.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllUsers() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllUserInfos() {
        em.createNativeQuery("TRUNCATE user_info CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllVeriffSessions() {
        em.createNativeQuery("DELETE FROM veriff_session").executeUpdate()
    }

    @Transactional
    fun deleteAllVeriffDecisions() {
        em.createNativeQuery("DELETE FROM veriff_decision").executeUpdate()
    }

    @Transactional
    fun deleteAllRefreshTokens() {
        em.createNativeQuery("DELETE FROM refresh_token").executeUpdate()
    }

    @Transactional
    fun deleteAllBlockchainTasks() {
        em.createNativeQuery("DELETE FROM blockchain_task").executeUpdate()
    }
}
