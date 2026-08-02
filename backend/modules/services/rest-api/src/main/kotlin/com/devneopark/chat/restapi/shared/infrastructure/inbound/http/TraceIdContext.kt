package com.devneopark.chat.restapi.shared.infrastructure.inbound.http

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

object TraceIdContext {

    suspend fun <T> with(traceId: String, block: suspend (String) -> T): T {
        val mdcContextMap = MDC.getCopyOfContextMap().orEmpty() + mapOf("traceId" to traceId)
        return withContext(MDCContext(mdcContextMap)) {
            block(traceId)
        }
    }

}