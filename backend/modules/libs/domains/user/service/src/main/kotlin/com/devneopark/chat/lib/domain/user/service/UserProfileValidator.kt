package com.devneopark.chat.lib.domain.user.service

/**
 * 사용자 프로필 정책 검증기.
 *
 * 외부에서 주입된 정규식을 사용해 표시 이름이 현재 사용자 프로필 정책을 만족하는지 확인한다.
 * 이 타입은 예외를 던지지 않고 검증 결과를 Boolean으로 반환하며, 실패 처리 방식은 호출 계층이 결정한다.
 *
 * @param displayNameRegex 표시 이름에 적용할 허용 형식 정규식.
 */
class UserProfileValidator(

    private val displayNameRegex: Regex

) {

    /**
     * 표시 이름이 사용자 프로필 정책을 만족하는지 확인한다.
     *
     * @param displayName 검증할 표시 이름.
     * @return 표시 이름이 [displayNameRegex]와 일치하면 `true`, 그렇지 않으면 `false`.
     */
    fun isValidDisplayName(displayName: CharSequence): Boolean {
        return displayNameRegex.matches(displayName)
    }

}
