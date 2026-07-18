package com.example.coffeeshoporder_system.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    // 전역 예외 핸들러가 HTTP 상태와 에러 코드를 결정할 때 사용하는 값입니다.
    private final ErrorCode errorCode;

    // ErrorCode에 정의된 기본 메시지로 예외를 생성합니다.
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 입력값별 상세 메시지가 필요할 때 기본 ErrorCode에 메시지만 덮어씁니다.
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
