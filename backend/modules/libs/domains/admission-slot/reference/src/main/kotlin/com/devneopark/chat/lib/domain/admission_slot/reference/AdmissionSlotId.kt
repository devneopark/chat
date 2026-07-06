package com.devneopark.chat.lib.domain.admission_slot.reference

/**
 * 채팅방 슬롯 도메인 경계에서 공유하는 슬롯 식별자 계약.
 *
 * 채팅방 슬롯 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 채팅방 슬롯 식별자 값만 참조한다.
 * 슬롯 식별자는 [roomId]와 [number] 두 필드로 구성된 복합키이며,
 * 두 값을 함께 사용해야 하나의 채팅방 슬롯을 식별할 수 있다.
 */
interface AdmissionSlotId {

    /**
     * 복합키에서 채팅방을 식별하는 값.
     */
    val roomId: String

    /**
     * 복합키에서 채팅방 내부의 슬롯을 구분하는 번호 값.
     */
    val number: Int

}
