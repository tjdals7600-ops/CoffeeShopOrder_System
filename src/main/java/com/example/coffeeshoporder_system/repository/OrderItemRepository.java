package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 주문 응답 변환 시 메뉴 참조를 함께 가져와 N+1 조회를 막습니다.
    @Query("select oi from OrderItem oi join fetch oi.menu where oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithMenu(@Param("orderId") Long orderId);

    // 특정 메뉴의 주문 항목을 확인할 때 사용하는 보조 조회입니다.
    List<OrderItem> findByMenu_Id(Long menuId);
}
