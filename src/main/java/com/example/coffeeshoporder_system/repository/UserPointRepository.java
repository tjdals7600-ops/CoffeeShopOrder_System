package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.UserPoint;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

    // 단순 잔액 조회나 멱등 응답 생성 시 사용합니다.
    Optional<UserPoint> findByUserId(Long userId);

    // 충전/결제 중 같은 사용자의 잔액을 동시에 변경하지 못하도록 비관적 락을 겁니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserPoint up where up.userId = :userId")
    Optional<UserPoint> findByUserIdForUpdate(@Param("userId") Long userId);
}
