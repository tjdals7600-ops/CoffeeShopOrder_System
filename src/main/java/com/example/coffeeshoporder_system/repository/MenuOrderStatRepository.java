package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.MenuOrderStat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuOrderStatRepository extends JpaRepository<MenuOrderStat, Long> {

    // 주문 성공 시 오늘 날짜의 메뉴별 집계 row를 찾아 누적합니다.
    Optional<MenuOrderStat> findByMenu_IdAndStatDate(Long menuId, LocalDate statDate);

    // 인기 메뉴 조회는 집계 테이블을 기간 합산해 주문 횟수 기준으로 정렬합니다.
    @Query("""
            select
                m.id as menuId,
                m.name as name,
                m.price as price,
                sum(s.orderCount) as orderCount,
                sum(s.totalQuantity) as totalQuantity,
                sum(s.totalSalesPoint) as totalSalesPoint
            from MenuOrderStat s
            join s.menu m
            where s.statDate between :startDate and :endDate
            group by m.id, m.name, m.price
            order by sum(s.orderCount) desc, sum(s.totalQuantity) desc
            """)
    List<PopularMenuStatProjection> findPopularMenus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
