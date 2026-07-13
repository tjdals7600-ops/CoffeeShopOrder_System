package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    List<OrderItem> findByMenu_Id(Long menuId);
}
