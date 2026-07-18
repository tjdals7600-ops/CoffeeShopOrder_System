package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.domain.entity.PointHistory;
import com.example.coffeeshoporder_system.domain.entity.UserPoint;
import com.example.coffeeshoporder_system.domain.type.PointHistoryType;
import com.example.coffeeshoporder_system.dto.request.PointChargeRequest;
import com.example.coffeeshoporder_system.dto.response.PointChargeResponse;
import com.example.coffeeshoporder_system.exception.CustomException;
import com.example.coffeeshoporder_system.exception.ErrorCode;
import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PointService {

    // 포인트 충전 이력에서 업무 사유를 구분하기 위한 고정값입니다.
    private static final String POINT_CHARGE_REASON = "POINT_CHARGE";

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // requestId 멱등성과 user_point row lock을 함께 사용해 중복 충전을 방지합니다.
    @Transactional
    public PointChargeResponse charge(PointChargeRequest request) {
        validateChargeRequest(request);

        // 이미 처리된 충전 요청이면 기존 충전 결과를 그대로 반환합니다.
        var duplicatedHistory = findChargeHistory(request);
        if (duplicatedHistory != null) {
            return toChargeResponse(duplicatedHistory);
        }

        // 잔액 변경 중 동시성 문제가 생기지 않도록 사용자 포인트 row를 잠급니다.
        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(request.userId())
                .orElseGet(() -> userPointRepository.save(UserPoint.builder()
                        .userId(request.userId())
                        .balance(0L)
                        .build()));

        // row lock을 잡기 전에 같은 requestId가 처리됐을 가능성을 다시 확인합니다.
        duplicatedHistory = findChargeHistory(request);
        if (duplicatedHistory != null) {
            return toChargeResponse(duplicatedHistory);
        }

        userPoint.charge(request.amount());

        // 충전 후 잔액을 이력에 남겨 재요청 시 DB 상태를 다시 계산하지 않고 응답할 수 있게 합니다.
        pointHistoryRepository.save(PointHistory.builder()
                .userId(request.userId())
                .type(PointHistoryType.CHARGE)
                .amount(request.amount())
                .balanceAfter(userPoint.getBalance())
                .reason(POINT_CHARGE_REASON)
                .requestId(request.requestId())
                .build());

        return new PointChargeResponse(userPoint.getUserId(), request.amount(), userPoint.getBalance());
    }

    // userId, requestId, type 조합으로 같은 충전 요청이 이미 반영됐는지 찾습니다.
    private PointHistory findChargeHistory(PointChargeRequest request) {
        return pointHistoryRepository.findByUserIdAndRequestIdAndType(
                request.userId(),
                request.requestId(),
                PointHistoryType.CHARGE
        ).orElse(null);
    }

    // 멱등 재요청에는 기존 point_history snapshot을 응답 DTO로 변환합니다.
    private PointChargeResponse toChargeResponse(PointHistory pointHistory) {
        return new PointChargeResponse(
                pointHistory.getUserId(),
                pointHistory.getAmount(),
                pointHistory.getBalanceAfter()
        );
    }

    // 서비스 레이어에서도 핵심 입력값을 검증해 컨트롤러 외 경로 호출을 방어합니다.
    private void validateChargeRequest(PointChargeRequest request) {
        if (request.userId() == null || request.userId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "사용자 식별값은 0보다 커야 합니다.");
        }
        if (!StringUtils.hasText(request.requestId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "requestId는 필수입니다.");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new CustomException(ErrorCode.INVALID_POINT_AMOUNT);
        }
    }
}
