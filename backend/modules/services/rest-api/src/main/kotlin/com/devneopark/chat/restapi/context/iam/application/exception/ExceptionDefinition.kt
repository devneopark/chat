package com.devneopark.chat.restapi.context.iam.application.exception

enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    DUPLICATED_PRINCIPAL("2-001-001", "Principal duplicated."),

    ;

}