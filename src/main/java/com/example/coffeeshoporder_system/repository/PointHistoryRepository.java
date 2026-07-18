package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.PointHistory;
import com.example.coffeeshoporder_system.domain.type.PointHistoryType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 특정 포인트 요청이 이미 처리됐는지 빠르게 확인할 때 사용합니다.
    boolean existsByUserIdAndRequestIdAndType(Long userId, String requestId, PointHistoryType type);

    // 멱등 재요청 응답을 위해 이전 포인트 이력 snapshot을 조회합니다.
    Optional<PointHistory> findByUserIdAndRequestIdAndType(Long userId, String requestId, PointHistoryType type);
}
