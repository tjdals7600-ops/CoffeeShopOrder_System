package com.example.coffeeshoporder_system.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class CreatedAtEntity {

    // 생성 시각만 필요한 이력성 테이블에서 사용합니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA persist 직전에 생성 시각을 채웁니다.
    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
