package com.devneopark.chat.lib.domain.user.service

class UserCredentialValidator(

    private val principalRegex: Regex,

    private val passwordRegex: Regex

) {

    fun isValidPrincipal(principal: CharSequence): Boolean {
        return principalRegex.matches(principal)
    }

    fun isValidPassword(password: CharSequence): Boolean {
        return passwordRegex.matches(password)
    }

}
