package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.entity.MenuOrderStat;
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
import com.example.coffeeshoporder_system.dto.request.OrderItemRequest;
import com.example.coffeeshoporder_system.dto.request.OrderRequest;
import com.example.coffeeshoporder_system.dto.response.OrderItemResponse;
import com.example.coffeeshoporder_system.dto.response.OrderResponse;
import com.example.coffeeshoporder_system.exception.CustomException;
import com.example.coffeeshoporder_system.exception.ErrorCode;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import com.example.coffeeshoporder_system.repository.MenuOrderStatRepository;
import com.example.coffeeshoporder_system.repository.OrderItemRepository;
import com.example.coffeeshoporder_system.repository.OrderOutboxRepository;
import com.example.coffeeshoporder_system.repository.OrderRepository;
import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String POINT_USE_REASON = "ORDER_PAYMENT";

    private final MenuRepository menuRepository;
    private final MenuOrderStatRepository menuOrderStatRepository;
    private final UserPointRepository userPointRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse order(OrderRequest request) {
        validateOrderRequest(request);

        Optional<Order> existingOrder = findExistingOrder(request);
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get(), getCurrentBalance(request.userId()));
        }

        List<RequestedOrderItem> requestedItems = normalizeItems(request);
        Map<Long, Menu> menusById = findMenusById(requestedItems);
        Long totalPrice = calculateTotalPrice(requestedItems, menusById);

        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        existingOrder = findExistingOrder(request);
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get(), userPoint.getBalance());
        }

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

        List<OrderItem> orderItems = orderItemRepository.saveAll(requestedItems.stream()
                .map(item -> toOrderItem(order, menusById.get(item.menuId()), item.quantity()))
                .toList());
        updateMenuOrderStats(orderItems);

        pointHistoryRepository.save(PointHistory.builder()
                .userId(request.userId())
                .type(PointHistoryType.USE)
                .amount(totalPrice)
                .balanceAfter(userPoint.getBalance())
                .reason(POINT_USE_REASON)
                .requestId(request.requestId())
                .build());

        OrderPaidEvent event = toOrderPaidEvent(order, orderItems);
        orderOutboxRepository.save(OrderOutbox.builder()
                .eventType(OutboxEventType.ORDER_PAID)
                .payload(toPayload(event))
                .status(OutboxStatus.READY)
                .retryCount(0)
                .build());
        orderEventPublisher.publishAfterCommit(event);

        return toResponse(order, userPoint.getBalance(), orderItems);
    }

    private Optional<Order> findExistingOrder(OrderRequest request) {
        return orderRepository.findByUserIdAndRequestId(request.userId(), request.requestId());
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.userId() == null || request.userId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "사용자 식별값은 0보다 커야 합니다.");
        }
        if (!StringUtils.hasText(request.requestId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "requestId는 필수입니다.");
        }
        if ((request.items() == null || request.items().isEmpty()) && request.menuId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "주문할 메뉴는 필수입니다.");
        }
    }

    private List<RequestedOrderItem> normalizeItems(OrderRequest request) {
        List<OrderItemRequest> requestItems = request.items();
        if (requestItems == null || requestItems.isEmpty()) {
            requestItems = List.of(new OrderItemRequest(request.menuId(), 1));
        }

        Map<Long, Integer> quantityByMenuId = new LinkedHashMap<>();
        for (OrderItemRequest item : requestItems) {
            if (item.menuId() == null || item.menuId() <= 0) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "메뉴 ID는 0보다 커야 합니다.");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new CustomException(ErrorCode.INVALID_ORDER_QUANTITY);
            }
            quantityByMenuId.merge(item.menuId(), item.quantity(), Integer::sum);
        }

        return quantityByMenuId.entrySet()
                .stream()
                .map(entry -> new RequestedOrderItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<Long, Menu> findMenusById(List<RequestedOrderItem> requestedItems) {
        List<Long> menuIds = requestedItems.stream()
                .map(RequestedOrderItem::menuId)
                .toList();
        Map<Long, Menu> menusById = menuRepository.findAllById(menuIds)
                .stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        for (Long menuId : menuIds) {
            Menu menu = menusById.get(menuId);
            if (menu == null) {
                throw new CustomException(ErrorCode.MENU_NOT_FOUND);
            }
            if (menu.getStatus() != MenuStatus.SELLING) {
                throw new CustomException(ErrorCode.MENU_NOT_SELLING);
            }
        }

        return menusById;
    }

    private Long calculateTotalPrice(List<RequestedOrderItem> requestedItems, Map<Long, Menu> menusById) {
        return requestedItems.stream()
                .mapToLong(item -> menusById.get(item.menuId()).getPrice() * item.quantity())
                .sum();
    }

    private OrderItem toOrderItem(Order order, Menu menu, Integer quantity) {
        Long linePrice = menu.getPrice() * quantity;
        return OrderItem.builder()
                .order(order)
                .menu(menu)
                .menuNameSnapshot(menu.getName())
                .menuPriceSnapshot(menu.getPrice())
                .quantity(quantity)
                .linePrice(linePrice)
                .build();
    }

    private OrderPaidEvent toOrderPaidEvent(Order order, List<OrderItem> orderItems) {
        return new OrderPaidEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getRequestId(),
                orderItems.stream()
                        .map(item -> new OrderPaidItemEvent(
                                item.getMenu().getId(),
                                item.getMenuNameSnapshot(),
                                item.getMenuPriceSnapshot(),
                                item.getQuantity(),
                                item.getLinePrice()
                        ))
                        .toList()
        );
    }

    private void updateMenuOrderStats(List<OrderItem> orderItems) {
        LocalDate statDate = LocalDate.now();
        for (OrderItem orderItem : orderItems) {
            MenuOrderStat stat = menuOrderStatRepository
                    .findByMenu_IdAndStatDate(orderItem.getMenu().getId(), statDate)
                    .orElseGet(() -> MenuOrderStat.builder()
                            .menu(orderItem.getMenu())
                            .statDate(statDate)
                            .orderCount(0L)
                            .totalQuantity(0L)
                            .totalSalesPoint(0L)
                            .build());

            stat.addOrder(orderItem.getQuantity(), orderItem.getLinePrice());
            menuOrderStatRepository.save(stat);
        }
    }

    private Long getCurrentBalance(Long userId) {
        return userPointRepository.findByUserId(userId)
                .map(UserPoint::getBalance)
                .orElse(0L);
    }

    private OrderResponse toResponse(Order order, Long balance) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderIdWithMenu(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalPrice(), balance, items);
    }

    private OrderResponse toResponse(Order order, Long balance, List<OrderItem> orderItems) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                balance,
                orderItems.stream()
                        .map(this::toItemResponse)
                        .toList()
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
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new CustomException(ErrorCode.OUTBOX_PROCESS_FAILED);
        }
    }

    private record RequestedOrderItem(
            Long menuId,
            Integer quantity
    ) {
    }
}
