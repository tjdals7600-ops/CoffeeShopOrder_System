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

    Optional<MenuOrderStat> findByMenu_IdAndStatDate(Long menuId, LocalDate statDate);

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
            order by sum(s.totalQuantity) desc, sum(s.orderCount) desc
            """)
    List<PopularMenuStatProjection> findPopularMenus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
