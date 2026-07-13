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

    List<Menu> findAllByStatusOrderByIdAsc(MenuStatus status);

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

    List<Menu> findAllByIdInAndStatus(Collection<Long> ids, MenuStatus status);

    Optional<Menu> findByIdAndStatus(Long id, MenuStatus status);
}
