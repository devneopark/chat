package com.devneopark.chat.restapi.shared.infrastructure.inbound.http

open class ExceptionResponse(

    override val code: String,

    open val message: String?

) : ApiResponseBase(code)
