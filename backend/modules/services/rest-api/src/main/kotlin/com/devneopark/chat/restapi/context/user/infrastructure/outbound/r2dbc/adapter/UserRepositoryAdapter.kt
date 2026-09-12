package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository.UserEntityRepository
import org.springframework.stereotype.Repository

/** User 도메인 객체와 R2DBC 영속성 모델 사이의 변환을 담당하는 저장소 어댑터다. */
@Repository
class UserRepositoryAdapter(

    private val userEntityRepository: UserEntityRepository

) : UserRepositoryPort {

    /** 활성 사용자 엔티티를 조회해 User 도메인 객체로 변환한다. */
    override suspend fun findById(id: UserId): User? {
        return userEntityRepository.findById(id.value)
            ?.toDomain()
    }

    /** User 도메인 객체의 프로필을 영속성 모델로 변환해 갱신한다. */
    override suspend fun updateProfile(user: User) {
        val entity = UserEntity.from(user)
        userEntityRepository.updateUserProfile(entity)
    }

}
