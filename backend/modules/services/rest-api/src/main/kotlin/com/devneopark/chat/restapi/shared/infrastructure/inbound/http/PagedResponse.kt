package com.devneopark.chat.restapi.shared.infrastructure.inbound.http

data class PagedResponse<T>(

    override val code: String,

    val contents: List<T> = listOf(),

    val currentPage: Int,

    val hasPrevious: Boolean,

    val hasNext: Boolean

) : ApiResponseBase(code)