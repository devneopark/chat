package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

abstract class ApiResponseBase(

    open val code: String? = "0-000-000"

)