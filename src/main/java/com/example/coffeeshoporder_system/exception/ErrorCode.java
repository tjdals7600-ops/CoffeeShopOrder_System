package com.example.coffeeshoporder_system.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // API 요청 검증, 도메인 상태, 외부 이벤트 처리 실패를 공통 에러 코드로 표현합니다.

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_POINT_AMOUNT", "포인트 금액은 0보다 커야 합니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_QUANTITY", "주문 수량은 0보다 커야 합니다."),

    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "존재하지 않는 메뉴입니다."),
    MENU_NOT_SELLING(HttpStatus.BAD_REQUEST, "MENU_NOT_SELLING", "판매 중인 메뉴만 주문할 수 있습니다."),
    USER_POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_POINT_NOT_FOUND", "사용자 포인트 정보를 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문 정보를 찾을 수 없습니다."),

    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT", "포인트 잔액이 부족합니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "DUPLICATE_REQUEST", "이미 처리된 요청입니다."),
    OUTBOX_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_PROCESS_FAILED", "주문 이벤트 처리에 실패했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    // 클라이언트에 반환할 HTTP 상태입니다.
    private final HttpStatus status;
    // 클라이언트와 테스트가 안정적으로 참조할 수 있는 에러 코드 문자열입니다.
    private final String code;
    // 기본 사용자-facing 에러 메시지입니다.
    private final String message;
}
