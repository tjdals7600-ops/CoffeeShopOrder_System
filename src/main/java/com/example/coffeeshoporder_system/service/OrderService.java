package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.entity.Order;
import com.example.coffeeshoporder_system.domain.entity.OrderItem;
import com.example.coffeeshoporder_system.domain.entity.OrderOutbox;
import com.example.coffeeshoporder_system.domain.entity.PointHistory;
import com.example.coffeeshoporder_system.domain.entity.UserPoint;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.domain.type.OrderStatus;
import com.example.coffeeshoporder_system.domain.type.OutboxEventType;
import com.example.coffeeshoporder_system.domain.type.OutboxStatus;
import com.example.coffeeshoporder_system.domain.type.PointHistoryType;
import com.example.coffeeshoporder_system.dto.request.OrderRequest;
import com.example.coffeeshoporder_system.dto.response.OrderItemResponse;
import com.example.coffeeshoporder_system.dto.response.OrderResponse;
import com.example.coffeeshoporder_system.exception.CustomException;
import com.example.coffeeshoporder_system.exception.ErrorCode;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import com.example.coffeeshoporder_system.repository.OrderItemRepository;
import com.example.coffeeshoporder_system.repository.OrderOutboxRepository;
import com.example.coffeeshoporder_system.repository.OrderRepository;
import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_QUANTITY = 1;
    private static final String POINT_USE_REASON = "ORDER_PAYMENT";

    private final MenuRepository menuRepository;
    private final UserPointRepository userPointRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final DataCollectionPlatformClient dataCollectionPlatformClient;

    @Transactional
    public OrderResponse order(OrderRequest request) {
        validateOrderRequest(request);

        var existingOrder = orderRepository.findByUserIdAndRequestId(request.userId(), request.requestId());
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get(), getCurrentBalance(request.userId()));
        }

        Menu menu = menuRepository.findById(request.menuId())
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        if (menu.getStatus() != MenuStatus.SELLING) {
            throw new CustomException(ErrorCode.MENU_NOT_SELLING);
        }
        Long totalPrice = menu.getPrice() * ORDER_QUANTITY;

        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));
        if (!userPoint.hasEnoughBalance(totalPrice)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINT);
        }
        userPoint.use(totalPrice);

        Order order = orderRepository.save(Order.builder()
                .userId(request.userId())
                .totalPrice(totalPrice)
                .status(OrderStatus.PAID)
                .requestId(request.requestId())
                .build());

        OrderItem orderItem = orderItemRepository.save(OrderItem.builder()
                .order(order)
                .menu(menu)
                .menuNameSnapshot(menu.getName())
                .menuPriceSnapshot(menu.getPrice())
                .quantity(ORDER_QUANTITY)
                .linePrice(totalPrice)
                .build());

        pointHistoryRepository.save(PointHistory.builder()
                .userId(request.userId())
                .type(PointHistoryType.USE)
                .amount(totalPrice)
                .balanceAfter(userPoint.getBalance())
                .reason(POINT_USE_REASON)
                .requestId(request.requestId())
                .build());

        OrderPaidEvent event = new OrderPaidEvent(
                order.getId(),
                order.getUserId(),
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                ORDER_QUANTITY,
                totalPrice,
                order.getRequestId()
        );
        orderOutboxRepository.save(OrderOutbox.builder()
                .eventType(OutboxEventType.ORDER_PAID)
                .payload(toPayload(event))
                .status(OutboxStatus.READY)
                .retryCount(0)
                .build());
        dataCollectionPlatformClient.send(event);

        return toResponse(order, userPoint.getBalance(), orderItem);
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.userId() == null || request.userId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "사용자 식별값은 0보다 커야 합니다.");
        }
        if (!StringUtils.hasText(request.requestId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "requestId는 필수입니다.");
        }
        if (request.menuId() == null || request.menuId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "메뉴 ID는 0보다 커야 합니다.");
        }
    }

    private Long getCurrentBalance(Long userId) {
        return userPointRepository.findByUserId(userId)
                .map(UserPoint::getBalance)
                .orElse(0L);
    }

    private OrderResponse toResponse(Order order, Long balance) {
        List<OrderItemResponse> items = orderItemRepository.findByOrder_Id(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalPrice(), balance, items);
    }

    private OrderResponse toResponse(Order order, Long balance, OrderItem orderItem) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                balance,
                List.of(toItemResponse(orderItem))
        );
    }

    private OrderItemResponse toItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getMenu().getId(),
                orderItem.getMenuNameSnapshot(),
                orderItem.getMenuPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getLinePrice()
        );
    }

    private String toPayload(OrderPaidEvent event) {
        return """
                {"orderId":%d,"userId":%d,"menuId":%d,"menuName":"%s","unitPrice":%d,"quantity":%d,"totalPrice":%d,"requestId":"%s"}
                """.formatted(
                event.orderId(),
                event.userId(),
                event.menuId(),
                event.menuName(),
                event.unitPrice(),
                event.quantity(),
                event.totalPrice(),
                event.requestId()
        );
    }
}
