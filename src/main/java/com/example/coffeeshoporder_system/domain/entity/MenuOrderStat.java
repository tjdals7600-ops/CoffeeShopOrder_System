package com.example.coffeeshoporder_system.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "menu_order_stat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_menu_order_stat_menu_date", columnNames = {"menu_id", "stat_date"})
        },
        indexes = {
                @Index(name = "idx_menu_order_stat_date_quantity", columnList = "stat_date, total_quantity")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuOrderStat {

    // 메뉴별 일자 집계 row의 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 집계 대상 메뉴입니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false, foreignKey = @ForeignKey(name = "fk_menu_order_stat_menu"))
    private Menu menu;

    // 집계 기준 일자입니다.
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    // 해당 일자에 이 메뉴가 주문된 횟수입니다.
    @Column(name = "order_count", nullable = false)
    private Long orderCount;

    // 해당 일자에 이 메뉴가 주문된 총 수량입니다.
    @Column(name = "total_quantity", nullable = false)
    private Long totalQuantity;

    // 해당 일자에 이 메뉴로 발생한 총 포인트 매출입니다.
    @Column(name = "total_sales_point", nullable = false)
    private Long totalSalesPoint;

    // 주문 성공 시 인기 메뉴 조회용 집계를 누적합니다.
    public void addOrder(Integer quantity, Long salesPoint) {
        orderCount += 1;
        totalQuantity += quantity;
        totalSalesPoint += salesPoint;
    }
}
