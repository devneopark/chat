package com.devneopark.chat.restapi.bootstrap.aop

import com.devneopark.chat.libs.shared.application.exception.ApplicationConflictException
import com.devneopark.chat.libs.shared.application.exception.ApplicationUnavailableException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionException

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class TransactionalUseCaseAdvice {

    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional)" +
                " && within(com.devneopark.chat.restapi.context..application.service..*)"
    )
    suspend fun wrap(joinPoint: ProceedingJoinPoint): Any? {
        try {
            return joinPoint.proceed()
        } catch (exception: DuplicateKeyException) {
            throw ApplicationConflictException(exception)
        } catch (exception: DataAccessException) {
            throw ApplicationUnavailableException(exception)
        } catch (exception: TransactionException) {
            throw ApplicationUnavailableException(exception)
        }
    }

}