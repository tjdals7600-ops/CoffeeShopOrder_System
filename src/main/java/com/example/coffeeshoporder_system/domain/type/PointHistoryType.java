package com.example.coffeeshoporder_system.domain.type;

public enum PointHistoryType {
    // 포인트 충전 이력입니다.
    CHARGE,

    // 주문 결제로 포인트를 사용한 이력입니다.
    USE,

    // 향후 취소/환불 흐름에서 사용할 수 있는 이력입니다.
    CANCEL
}
