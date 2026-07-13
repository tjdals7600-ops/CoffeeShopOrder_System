package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.PointHistory;
import com.example.coffeeshoporder_system.domain.type.PointHistoryType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    boolean existsByUserIdAndRequestIdAndType(Long userId, String requestId, PointHistoryType type);

    Optional<PointHistory> findByUserIdAndRequestIdAndType(Long userId, String requestId, PointHistoryType type);
}
