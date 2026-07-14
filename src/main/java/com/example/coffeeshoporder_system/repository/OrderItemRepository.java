package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi from OrderItem oi join fetch oi.menu where oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithMenu(@Param("orderId") Long orderId);

    List<OrderItem> findByMenu_Id(Long menuId);
}
