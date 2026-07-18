package com.example.coffeeshoporder_system.exception;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 의도적으로 던진 예외는 ErrorCode에 맞춰 응답합니다.
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode, exception.getMessage()));
    }

    // enum 변환 실패 등 잘못된 인자 오류는 공통 INVALID_REQUEST로 변환합니다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode, exception.getMessage()));
    }

    // 처리하지 못한 예외는 내부 로그만 남기고 공통 서버 오류 응답으로 감춥니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unhandled exception occurred.", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode));
    }

    // API 에러 응답의 공통 JSON 구조입니다.
    private record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String code,
            String message
    ) {

        // ErrorCode 기본 메시지를 그대로 사용하는 응답을 만듭니다.
        private static ErrorResponse from(ErrorCode errorCode) {
            return from(errorCode, errorCode.getMessage());
        }

        // 상황별 상세 메시지를 포함한 응답을 만듭니다.
        private static ErrorResponse from(ErrorCode errorCode, String message) {
            return new ErrorResponse(
                    LocalDateTime.now(),
                    errorCode.getStatus().value(),
                    errorCode.getCode(),
                    message
            );
        }
    }
}
