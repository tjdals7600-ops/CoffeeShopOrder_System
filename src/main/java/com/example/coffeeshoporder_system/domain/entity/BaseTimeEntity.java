package com.example.coffeeshoporder_system.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    // 엔티티가 처음 저장된 시각입니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 엔티티가 마지막으로 수정된 시각입니다.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA persist 직전에 생성/수정 시각을 함께 채웁니다.
    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // JPA update 직전에 수정 시각만 갱신합니다.
    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
