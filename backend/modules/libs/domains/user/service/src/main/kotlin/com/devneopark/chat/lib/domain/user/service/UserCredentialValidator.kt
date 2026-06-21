package com.devneopark.chat.lib.domain.user.service

/**
 * 사용자 인증 정보 정책 검증기.
 *
 * 외부에서 주입된 정규식을 사용해 principal 값과 원문 비밀번호가 현재 사용자 정책을 만족하는지 확인한다.
 * 이 타입은 예외를 던지지 않고 검증 결과를 Boolean으로 반환하며, 실패 처리 방식은 호출 계층이 결정한다.
 *
 * @param principalRegex principal 값에 적용할 허용 형식 정규식.
 * @param passwordRegex 원문 비밀번호에 적용할 허용 형식 정규식.
 */
class UserCredentialValidator(

    private val principalRegex: Regex,

    private val passwordRegex: Regex

) {

    /**
     * principal 값이 사용자 인증 정책을 만족하는지 확인한다.
     *
     * @param principal 검증할 principal 값.
     * @return principal 값이 [principalRegex]와 일치하면 `true`, 그렇지 않으면 `false`.
     */
    fun isValidPrincipal(principal: CharSequence): Boolean {
        return principalRegex.matches(principal)
    }

    /**
     * 원문 비밀번호가 사용자 인증 정책을 만족하는지 확인한다.
     *
     * @param password 검증할 원문 비밀번호.
     * @return 비밀번호가 [passwordRegex]와 일치하면 `true`, 그렇지 않으면 `false`.
     */
    fun isValidPassword(password: CharSequence): Boolean {
        return passwordRegex.matches(password)
    }

}
