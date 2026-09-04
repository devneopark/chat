package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

class FieldBindingExceptionResponse(

    override val code: String,

    override val message: String?,

    val fields: List<String>

) : ExceptionResponse(code, message)
