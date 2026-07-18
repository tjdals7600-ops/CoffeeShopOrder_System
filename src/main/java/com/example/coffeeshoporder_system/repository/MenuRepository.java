package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // 판매 상태별 메뉴 목록을 엔티티로 조회할 때 사용합니다.
    List<Menu> findAllByStatusOrderByIdAsc(MenuStatus status);

    // 메뉴 목록 API는 필요한 컬럼만 projection으로 조회해 불필요한 엔티티 로딩을 줄입니다.
    @Query("""
            select
                m.id as menuId,
                m.name as name,
                m.price as price
            from Menu m
            where m.status = :status
            order by m.id asc
            """)
    List<MenuSummaryProjection> findMenuSummariesByStatusOrderByIdAsc(@Param("status") MenuStatus status);

    // 여러 메뉴를 상태 조건과 함께 한 번에 확인할 때 사용합니다.
    List<Menu> findAllByIdInAndStatus(Collection<Long> ids, MenuStatus status);

    // 단건 메뉴가 판매 중인지 확인할 때 사용합니다.
    Optional<Menu> findByIdAndStatus(Long id, MenuStatus status);
}
