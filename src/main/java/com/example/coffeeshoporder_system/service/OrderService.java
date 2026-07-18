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

    // 포인트 사용 이력에서 주문 결제를 구분하기 위한 고정값입니다.
    private static final String POINT_USE_REASON = "ORDER_PAYMENT";

    private final MenuRepository menuRepository;
    private final MenuOrderStatRepository menuOrderStatRepository;
    private final UserPointRepository userPointRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final CacheInvalidationService cacheInvalidationService;
    private final ObjectMapper objectMapper;

    // 주문 생성, 포인트 차감, 주문 이력 저장, outbox 저장을 하나의 트랜잭션으로 처리합니다.
    @Transactional
    public OrderResponse order(OrderRequest request) {
        validateOrderRequest(request);

        // 같은 requestId의 주문이 이미 있으면 중복 결제를 하지 않고 기존 주문 결과를 반환합니다.
        Optional<Order> existingOrder = findExistingOrder(request);
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get(), getCurrentBalance(request.userId()));
        }

        // 단일 메뉴 요청과 다중 메뉴 요청을 하나의 내부 형식으로 맞춥니다.
        List<RequestedOrderItem> requestedItems = normalizeItems(request);
        // 주문 금액은 클라이언트 입력을 신뢰하지 않고 DB 메뉴 가격으로 계산합니다.
        Map<Long, Menu> menusById = findMenusById(requestedItems);
        Long totalPrice = calculateTotalPrice(requestedItems, menusById);

        // 결제 중 잔액 경쟁 조건을 막기 위해 사용자 포인트 row를 잠급니다.
        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_POINT_NOT_FOUND));

        // row lock 이후에도 멱등 요청 여부를 다시 확인해 동시 요청을 방어합니다.
        existingOrder = findExistingOrder(request);
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get(), userPoint.getBalance());
        }

        if (!userPoint.hasEnoughBalance(totalPrice)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINT);
        }
        userPoint.use(totalPrice);

        // 주문은 포인트 결제까지 완료된 PAID 상태로 저장합니다.
        Order order = orderRepository.save(Order.builder()
                .userId(request.userId())
                .totalPrice(totalPrice)
                .status(OrderStatus.PAID)
                .requestId(request.requestId())
                .build());

        // 주문 당시 메뉴명/가격 snapshot을 order_item에 저장합니다.
        List<OrderItem> orderItems = orderItemRepository.saveAll(requestedItems.stream()
                .map(item -> toOrderItem(order, menusById.get(item.menuId()), item.quantity()))
                .toList());
        updateMenuOrderStats(orderItems);

        // 결제 후 잔액을 포인트 이력에 남깁니다.
        pointHistoryRepository.save(PointHistory.builder()
                .userId(request.userId())
                .type(PointHistoryType.USE)
                .amount(totalPrice)
                .balanceAfter(userPoint.getBalance())
                .reason(POINT_USE_REASON)
                .requestId(request.requestId())
                .build());

        // 외부 전송 실패와 주문 트랜잭션을 분리하기 위해 outbox 이벤트를 먼저 저장합니다.
        OrderPaidEvent event = toOrderPaidEvent(order, orderItems);
        orderOutboxRepository.save(OrderOutbox.builder()
                .eventType(OutboxEventType.ORDER_PAID)
                .payload(toPayload(event))
                .status(OutboxStatus.READY)
                .retryCount(0)
                .build());
        // 커밋된 주문만 외부 수집 플랫폼과 인기 메뉴 캐시에 반영합니다.
        orderEventPublisher.publishAfterCommit(event);
        cacheInvalidationService.evictPopularMenusAfterCommit();

        return toResponse(order, userPoint.getBalance(), orderItems);
    }

    // userId와 requestId의 unique 제약과 같은 기준으로 기존 주문을 조회합니다.
    private Optional<Order> findExistingOrder(OrderRequest request) {
        return orderRepository.findByUserIdAndRequestId(request.userId(), request.requestId());
    }

    // 주문 처리에 필요한 최소 입력값을 서비스 경계에서 검증합니다.
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

    // 단일 메뉴 주문 필드와 items 배열을 동일한 주문 항목 목록으로 정규화합니다.
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

    // 필요한 메뉴를 한 번에 조회해 주문 항목별 조회로 인한 N+1 문제를 피합니다.
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

    // DB에서 조회한 현재 메뉴 가격과 요청 수량으로 총 주문 금액을 계산합니다.
    private Long calculateTotalPrice(List<RequestedOrderItem> requestedItems, Map<Long, Menu> menusById) {
        return requestedItems.stream()
                .mapToLong(item -> menusById.get(item.menuId()).getPrice() * item.quantity())
                .sum();
    }

    // 주문 항목에는 이후 메뉴 정보가 바뀌어도 주문 당시 값이 남도록 snapshot을 저장합니다.
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

    // 데이터 수집 플랫폼으로 보낼 주문 완료 이벤트를 구성합니다.
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

    // 인기 메뉴 API가 주문 원장을 직접 스캔하지 않도록 메뉴별 일자 집계를 갱신합니다.
    private void updateMenuOrderStats(List<OrderItem> orderItems) {
        LocalDate statDate = LocalDate.now();
        for (OrderItem orderItem : orderItems) {
            // 같은 메뉴와 날짜의 집계 row가 있으면 누적하고, 없으면 새로 만듭니다.
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

    // 기존 주문 결과를 반환할 때 최신 잔액을 함께 제공합니다.
    private Long getCurrentBalance(Long userId) {
        return userPointRepository.findByUserId(userId)
                .map(UserPoint::getBalance)
                .orElse(0L);
    }

    // 멱등 재요청 응답을 위해 저장된 주문 항목을 다시 조회해 DTO로 변환합니다.
    private OrderResponse toResponse(Order order, Long balance) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderIdWithMenu(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalPrice(), balance, items);
    }

    // 방금 저장한 주문 항목을 재조회하지 않고 즉시 응답 DTO로 변환합니다.
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

    // Entity가 컨트롤러 응답으로 직접 노출되지 않도록 주문 항목 DTO로 변환합니다.
    private OrderItemResponse toItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getMenu().getId(),
                orderItem.getMenuNameSnapshot(),
                orderItem.getMenuPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getLinePrice()
        );
    }

    // outbox payload는 JSON 문자열로 저장해 후속 worker나 외부 연동이 그대로 사용할 수 있게 합니다.
    private String toPayload(OrderPaidEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new CustomException(ErrorCode.OUTBOX_PROCESS_FAILED);
        }
    }

    // 서비스 내부에서만 사용하는 정규화된 주문 항목 값 객체입니다.
    private record RequestedOrderItem(
            Long menuId,
            Integer quantity
    ) {
    }
}
