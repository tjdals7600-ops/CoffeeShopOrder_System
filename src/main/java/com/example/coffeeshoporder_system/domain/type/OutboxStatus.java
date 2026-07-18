package com.example.coffeeshoporder_system.domain.type;

public enum OutboxStatus {
    // 아직 처리되지 않은 outbox 이벤트입니다.
    READY,

    // 외부 전송이 완료된 outbox 이벤트입니다.
    DONE,

    // 재시도 후에도 처리에 실패한 outbox 이벤트입니다.
    FAILED
}
