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

    private static final String POINT_CHARGE_REASON = "POINT_CHARGE";

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public PointChargeResponse charge(PointChargeRequest request) {
        validateChargeRequest(request);

        var duplicatedHistory = findChargeHistory(request);
        if (duplicatedHistory != null) {
            return toChargeResponse(duplicatedHistory);
        }

        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(request.userId())
                .orElseGet(() -> userPointRepository.save(UserPoint.builder()
                        .userId(request.userId())
                        .balance(0L)
                        .build()));

        duplicatedHistory = findChargeHistory(request);
        if (duplicatedHistory != null) {
            return toChargeResponse(duplicatedHistory);
        }

        userPoint.charge(request.amount());

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

    private PointHistory findChargeHistory(PointChargeRequest request) {
        return pointHistoryRepository.findByUserIdAndRequestIdAndType(
                request.userId(),
                request.requestId(),
                PointHistoryType.CHARGE
        ).orElse(null);
    }

    private PointChargeResponse toChargeResponse(PointHistory pointHistory) {
        return new PointChargeResponse(
                pointHistory.getUserId(),
                pointHistory.getAmount(),
                pointHistory.getBalanceAfter()
        );
    }

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
