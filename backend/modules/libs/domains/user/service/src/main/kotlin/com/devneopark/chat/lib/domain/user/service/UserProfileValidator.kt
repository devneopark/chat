package com.devneopark.chat.lib.domain.user.service

class UserProfileValidator(

    private val displayNameRegex: Regex

) {

    fun isValidDisplayName(displayName: CharSequence): Boolean {
        return displayNameRegex.matches(displayName)
    }

}
