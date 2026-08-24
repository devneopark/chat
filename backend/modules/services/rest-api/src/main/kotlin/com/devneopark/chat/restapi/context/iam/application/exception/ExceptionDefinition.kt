package com.devneopark.chat.restapi.context.iam.application.exception

enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    DUPLICATED_PRINCIPAL("2-001-001", "Principal duplicated."),

    USER_NOT_FOUND("2-001-002", "User not found."),

    WRONG_PASSWORD("2-001-003", "Wrong password."),

    ;

}