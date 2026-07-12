<img width="1448" height="1086" alt="coffee_erd" src="https://github.com/user-attachments/assets/bff0a6cf-aaf1-430c-8974-ab31c9c1269e" /># 다수 서버 환경에서도 안정적으로 동작하는 커피숍 주문 시스템

## 1. 프로젝트 목표

이 시스템은 여러 대의 API 서버가 동시에 요청을 처리하는 환경에서도 **포인트 기반 커피 주문을 안정적으로 처리**하는 것을 목표로 한다.

핵심 요구사항은 다음과 같다.

1. 커피 주문에 필요한 메뉴를 구성하고 조회할 수 있어야 한다.
2. 커피 주문은 포인트로만 가능해야 한다.
3. 커피 주문 내역을 기반으로 인기 있는 메뉴를 추천해야 한다.
4. 다수 서버 환경에서도 포인트 차감, 주문 생성, 인기 메뉴 집계가 데이터 정합성을 깨지 않고 동작해야 한다.

이 README는 단순 기능 목록이 아니라, **왜 이런 구조로 설계했는지**, 그리고 **동시성, 데이터 일관성, 확장성 관점에서 어떤 선택을 했는지**를 설명하는 설계 문서이다.

---

## 2. 필수 API 목록

| 기능 | Method | Endpoint | 설명 |
|---|---:|---|---|
| 커피 메뉴 목록 조회 API | GET | `/api/menus` | 주문 가능한 커피 메뉴 목록을 조회한다. |
| 포인트 충전하기 API | POST | `/api/points/charge` | 사용자의 포인트를 충전한다. |
| 커피 주문, 결제하기 API | POST | `/api/orders` | 포인트를 차감하고 주문을 생성한다. |
| 인기 메뉴 목록 조회 API | GET | `/api/menus/popular` | 주문 내역 기반 인기 메뉴를 조회한다. |

---

## ERD

<img width="1448" height="1086" alt="coffee_erd" src="https://github.com/user-attachments/assets/c0c66fd4-0b3a-4dc6-8207-a5f1a52a2e4a" />


---

## API 명세

<img width="1055" height="1491" alt="coffee_api" src="https://github.com/user-attachments/assets/2bfc10d1-feee-4b47-9cd0-4c43e4e547eb" />


---

# 3. 설계 내용

## 3.1 전체 구조

```text
Client
  |
  v
Load Balancer
  |
  +-------------------+-------------------+
  |                   |                   |
  v                   v                   v
API Server #1     API Server #2     API Server #3
  |                   |                   |
  +---------+---------+---------+---------+
            |
            v
        RDBMS(MySQL)
            |
            +---- user_point
            +---- point_history
            +---- menu
            +---- orders
            +---- order_item
            +---- order_outbox
            +---- menu_order_stat

Redis
  |
  +---- menu cache
  +---- popular menu cache/ranking

Batch/Worker
  |
  +---- order_outbox 소비
  +---- menu_order_stat 갱신
```

### 핵심 설계 방향

- API 서버는 **Stateless**하게 구성한다.
- 포인트 잔액, 주문, 결제 결과는 **RDBMS를 단일 진실 공급원(Source of Truth)** 으로 둔다.
- 포인트 차감과 주문 생성은 반드시 **하나의 DB 트랜잭션** 안에서 처리한다.
- 인기 메뉴 조회는 주문/결제와 달리 즉시 강한 일관성이 필수는 아니므로, **집계 테이블 + 캐시**를 사용한다.
- 다수 서버 환경에서 인메모리 락은 사용하지 않는다. 서버별 메모리가 분리되어 있기 때문에 동시성 제어에 사용할 수 없다.
- 포인트 차감처럼 돈과 직접 연결되는 데이터는 Redis 락보다 **DB row lock + unique constraint + transaction**을 우선 사용한다.

---

## 3.2 도메인 모델

## Menu

커피 메뉴 정보를 관리한다.

```text
menu
- id
- name
- price
- description
- status       // 판매중, 판매중지
- created_at
- updated_at
```

설계 포인트:

- 메뉴 가격은 주문 시점에 DB에서 다시 조회한다.
- 클라이언트가 보낸 가격은 신뢰하지 않는다.
- 판매 중지된 메뉴는 주문할 수 없다.

---

## UserPoint

사용자의 현재 포인트 잔액을 관리한다.

```text
user_point
- id
- user_id
- balance
- created_at
- updated_at
```

설계 포인트:

- 주문 결제 시 `user_point` row에 비관적 락을 건다.
- 같은 사용자가 동시에 여러 주문을 보내도 포인트가 음수가 되지 않도록 한다.
- 포인트 잔액은 조회 성능을 위해 별도 테이블로 관리하고, 변경 이력은 `point_history`에 남긴다.

---

## PointHistory

포인트 충전/사용 이력을 저장한다.

```text
point_history
- id
- user_id
- type         // CHARGE, USE, CANCEL
- amount
- balance_after
- reason
- request_id
- created_at
```

설계 포인트:

- 현재 잔액만 저장하면 장애나 CS 문의 상황에서 원인을 추적하기 어렵다.
- 모든 포인트 변경은 이력으로 남긴다.
- `request_id`를 저장하여 같은 요청이 중복 처리되는 것을 방지한다.

---

## Order

주문의 대표 정보를 저장한다.

```text
orders
- id
- user_id
- total_price
- status       // PAID, CANCELED
- request_id
- created_at
```

설계 포인트:

- 주문은 결제 성공 후 `PAID` 상태로 저장한다.
- 포인트가 부족하거나 메뉴가 유효하지 않으면 주문 자체를 생성하지 않는다.
- `user_id + request_id`에 unique constraint를 걸어 중복 주문을 방지한다.

---

## OrderItem

주문에 포함된 메뉴 정보를 저장한다.

```text
order_item
- id
- order_id
- menu_id
- menu_name_snapshot
- menu_price_snapshot
- quantity
- line_price
```

설계 포인트:

- 주문 당시의 메뉴명과 가격을 snapshot으로 저장한다.
- 이후 메뉴 가격이나 이름이 변경되어도 과거 주문 내역은 변하지 않아야 한다.
- 인기 메뉴 집계는 `order_item.menu_id`를 기준으로 수행한다.

---

## OrderOutbox

주문 완료 이벤트를 안정적으로 처리하기 위한 이벤트 저장 테이블이다.

```text
order_outbox
- id
- event_type
- payload
- status       // READY, DONE, FAILED
- retry_count
- created_at
```

설계 포인트:

- 주문 트랜잭션 안에서 outbox 이벤트를 함께 저장한다.
- 주문은 성공했는데 인기 메뉴 집계 이벤트만 유실되는 문제를 방지한다.
- Worker가 outbox를 읽어서 인기 메뉴 집계 테이블을 갱신한다.

---

## MenuOrderStat

인기 메뉴 조회를 빠르게 하기 위한 집계 테이블이다.

```text
menu_order_stat
- id
- menu_id
- stat_date
- order_count
- total_quantity
- total_sales_point
```

설계 포인트:

- 인기 메뉴 API가 매번 전체 주문 테이블을 group by 하지 않도록 한다.
- 주문이 많아져도 인기 메뉴 조회 성능을 일정하게 유지한다.
- 일간/주간/월간 기준으로 확장하기 쉽다.

---

# 4. 설계의 의도

## 4.1 주문/결제는 강한 일관성이 필요하다

포인트는 현금성 데이터에 가깝다. 따라서 커피 주문 과정에서 포인트가 잘못 차감되거나, 잔액보다 더 많은 금액이 사용되면 시스템 신뢰도가 크게 떨어진다.

그래서 주문/결제는 다음 원칙으로 설계한다.

```text
포인트 조회 → 잔액 검증 → 포인트 차감 → 주문 생성 → 포인트 이력 저장
```

이 전체 과정을 **하나의 DB 트랜잭션**으로 묶는다.

어느 하나라도 실패하면 전체를 rollback한다.

예를 들어 주문 생성은 성공했는데 포인트 차감이 실패하거나, 포인트는 차감됐는데 주문이 생성되지 않는 상태를 허용하지 않는다.

---

## 4.2 인기 메뉴는 최종적 일관성으로 충분하다

인기 메뉴 추천은 주문/결제와 성격이 다르다.

주문 직후 1초 이내에 인기 메뉴 순위에 반영되지 않아도 서비스 신뢰성이 크게 훼손되지는 않는다. 반면 인기 메뉴 API가 매번 주문 전체 데이터를 집계하면 트래픽이 증가할수록 DB 부하가 커진다.

따라서 인기 메뉴는 다음 방식으로 설계한다.

```text
주문 완료
  → order_outbox 저장
  → Worker가 이벤트 처리
  → menu_order_stat 집계
  → Redis에 인기 메뉴 캐싱
  → 인기 메뉴 API에서 빠르게 조회
```

즉, 주문/결제는 강한 일관성을 선택하고, 인기 메뉴는 성능과 확장성을 위해 최종적 일관성을 선택한다.

---

## 4.3 API 서버는 상태를 가지지 않는다

다수 서버 환경에서는 같은 사용자의 요청이 항상 같은 서버로 들어온다는 보장이 없다.

따라서 API 서버 메모리에 다음과 같은 상태를 저장하지 않는다.

- 사용자 포인트 잔액
- 주문 진행 상태
- 중복 요청 여부
- 인기 메뉴 순위의 원본 데이터

이 정보들은 DB 또는 Redis에 저장한다.

API 서버는 요청을 받아 검증하고, DB 트랜잭션을 실행하고, 응답을 반환하는 역할에 집중한다.

이렇게 하면 서버를 1대에서 3대, 10대로 늘려도 구조가 크게 바뀌지 않는다.

---

# 5. 선택한 문제해결 전략 및 분석한 내용

## 5.1 포인트 동시성 문제

### 문제 상황

사용자의 현재 포인트가 5,000P이고, 동시에 4,000P 주문 요청이 2번 들어온다고 가정한다.

```text
현재 잔액: 5,000P
요청 A: 아메리카노 4,000P 주문
요청 B: 라떼 4,000P 주문
```

동시성 제어가 없다면 두 요청이 모두 5,000P 잔액을 보고 결제에 성공할 수 있다.

결과적으로 총 8,000P를 사용했지만 실제 보유 포인트는 5,000P였던 문제가 발생한다.

---

### 선택한 전략: DB 비관적 락

주문 결제 시 사용자의 `user_point` row를 `SELECT ... FOR UPDATE` 방식으로 잠근다.

```text
Transaction A 시작
  user_point row lock 획득
  잔액 5,000P 확인
  4,000P 차감
  잔액 1,000P 저장
Transaction A commit

Transaction B 대기
  lock 해제 후 잔액 1,000P 확인
  4,000P 결제 불가
Transaction B rollback
```

이 전략을 선택한 이유는 다음과 같다.

- 포인트는 금전성 데이터라 정확성이 성능보다 중요하다.
- 같은 사용자의 포인트 row만 잠그므로 전체 시스템 락이 아니다.
- 여러 API 서버에서 동시에 요청이 들어와도 DB가 락을 관리하므로 안정적이다.
- Redis 분산 락보다 DB 트랜잭션과 함께 일관성을 보장하기 쉽다.

---

## 5.2 중복 주문 문제

### 문제 상황

사용자가 주문 버튼을 두 번 누르거나, 네트워크 타임아웃으로 클라이언트가 같은 요청을 재시도할 수 있다.

이때 서버가 같은 주문을 두 번 처리하면 포인트도 두 번 차감된다.

---

### 선택한 전략: Idempotency Key

클라이언트는 주문 요청 시 `requestId`를 함께 보낸다.

```json
{
  "requestId": "20260709-user1-order-001",
  "items": [
    {
      "menuId": 1,
      "quantity": 2
    }
  ]
}
```

서버는 `orders` 테이블에 다음 unique constraint를 둔다.

```text
unique(user_id, request_id)
```

같은 사용자가 같은 `requestId`로 다시 요청하면 새 주문을 생성하지 않고, 기존 주문 결과를 반환한다.

이 전략을 선택한 이유는 다음과 같다.

- 네트워크 재시도는 실제 운영 환경에서 흔하다.
- 중복 요청을 API 서버 메모리에서 막으면 다수 서버 환경에서 깨진다.
- DB unique constraint는 여러 서버가 동시에 요청을 받아도 동일하게 동작한다.

---

## 5.3 메뉴 가격 변경 문제

### 문제 상황

주문 당시 아메리카노 가격이 4,000P였는데, 다음 날 4,500P로 변경될 수 있다.

이때 과거 주문 내역이 현재 메뉴 가격을 참조하면 과거 주문 금액이 바뀐 것처럼 보이는 문제가 생긴다.

---

### 선택한 전략: 주문 시점의 메뉴 정보 snapshot 저장

`order_item`에 주문 당시 메뉴명과 가격을 저장한다.

```text
menu_name_snapshot
menu_price_snapshot
```

이 전략을 선택한 이유는 다음과 같다.

- 주문 내역은 과거 사실을 나타내는 데이터이다.
- 현재 메뉴 정보 변경이 과거 주문에 영향을 주면 안 된다.
- CS, 환불, 회계성 데이터 확인 시 주문 당시 금액이 필요하다.

---

## 5.4 인기 메뉴 조회 성능 문제

### 문제 상황

인기 메뉴 목록을 조회할 때마다 `orders`, `order_item`, `menu`를 join하고 group by 하면 주문 데이터가 많아질수록 DB 부하가 커진다.

```sql
SELECT menu_id, SUM(quantity)
FROM order_item
GROUP BY menu_id
ORDER BY SUM(quantity) DESC;
```

초기 데이터가 적을 때는 문제가 없어 보이지만, 주문 수가 많아질수록 인기 메뉴 API 하나가 전체 서비스 성능을 떨어뜨릴 수 있다.

---

### 선택한 전략: 집계 테이블 + Redis 캐시

주문 완료 후 `menu_order_stat` 테이블에 메뉴별 주문 수량을 누적한다.

인기 메뉴 API는 매번 원본 주문 테이블을 집계하지 않고, 집계된 데이터를 조회한다.

추가로 Redis에 인기 메뉴 결과를 짧은 TTL로 캐싱한다.

```text
GET /api/menus/popular
  → Redis cache 확인
  → cache hit: 바로 반환
  → cache miss: menu_order_stat 조회
  → Redis 저장 후 반환
```

이 전략을 선택한 이유는 다음과 같다.

- 인기 메뉴는 실시간 결제만큼 강한 일관성이 필요하지 않다.
- 읽기 요청이 많아져도 DB 부하를 줄일 수 있다.
- 집계 기준을 일간, 주간, 월간으로 확장하기 쉽다.

---

## 5.5 주문 성공 후 집계 실패 문제

### 문제 상황

주문 트랜잭션은 성공했지만, 인기 메뉴 집계 이벤트 발행에 실패할 수 있다.

예를 들어 주문은 DB에 저장되었는데 Redis 업데이트나 메시지 발행이 실패하면 인기 메뉴 통계가 누락된다.

---

### 선택한 전략: Transactional Outbox Pattern

주문을 저장하는 트랜잭션 안에서 `order_outbox`에도 이벤트를 함께 저장한다.

```text
주문 트랜잭션
  - orders 저장
  - order_item 저장
  - point 차감
  - point_history 저장
  - order_outbox 저장
commit
```

그 후 Worker가 `order_outbox`를 읽어 집계 작업을 수행한다.

이 전략을 선택한 이유는 다음과 같다.

- 주문 데이터와 이벤트 저장의 원자성을 보장할 수 있다.
- Worker가 실패해도 outbox에 이벤트가 남아 재처리할 수 있다.
- 메시지 브로커를 도입하더라도 DB와 브로커 간 dual-write 문제를 줄일 수 있다.

---

# 6. 기술적 선택 이유

## 6.1 RDBMS를 핵심 저장소로 선택한 이유

포인트, 주문, 결제는 정합성이 중요하다.

따라서 다음 기능이 필요한데, RDBMS가 이 요구사항에 적합하다.

- Transaction
- Row Lock
- Unique Constraint
- Foreign Key
- Index
- 정규화된 데이터 모델링

특히 포인트 차감과 주문 생성은 하나의 트랜잭션으로 묶어야 하기 때문에 RDBMS를 중심에 둔다.

---

## 6.2 비관적 락을 선택한 이유

포인트 결제에서 가장 중요한 것은 `잔액이 음수가 되면 안 된다`는 점이다.

낙관적 락도 사용할 수 있지만, 충돌이 발생하면 재시도 로직이 필요하다. 주문 결제는 사용자가 체감하는 핵심 흐름이므로 실패 후 재시도보다 한 요청이 명확하게 먼저 처리되고, 나머지가 최신 잔액을 확인하는 방식이 더 예측 가능하다.

따라서 포인트 결제에는 비관적 락을 사용한다.

단, 락 범위는 사용자의 `user_point` row 하나로 제한한다.

즉, 전체 테이블이나 전체 주문 시스템을 잠그지 않는다.

---

## 6.3 Redis를 캐시와 랭킹 조회에 사용하는 이유

Redis는 빠르지만, 포인트 차감의 최종 저장소로 두기에는 장애 복구와 트랜잭션 정합성 관리가 복잡해질 수 있다.

따라서 Redis는 다음 용도로 제한한다.

- 메뉴 목록 캐시
- 인기 메뉴 목록 캐시
- 인기 메뉴 랭킹 조회 최적화

포인트 잔액의 최종 기준은 반드시 DB로 둔다.

이렇게 하면 Redis 장애가 발생해도 주문/결제 정합성은 유지된다.

---

## 6.4 Transactional Outbox를 선택한 이유

주문 성공 후 인기 메뉴 집계를 비동기로 처리하면 성능은 좋아지지만 이벤트 유실 위험이 생긴다.

단순히 주문 저장 후 메시지를 발행하는 구조는 다음 문제가 있다.

```text
주문 DB 저장 성공
메시지 발행 실패
→ 주문은 있는데 인기 메뉴 집계 이벤트는 없음
```

반대로 메시지를 먼저 발행하고 주문 저장이 실패해도 문제가 된다.

그래서 주문 트랜잭션 안에서 outbox 이벤트를 함께 저장한다.

DB commit이 성공했다면 이벤트도 반드시 DB에 남아 있고, Worker는 이 이벤트를 재시도할 수 있다.

---

## 6.5 API 서버를 Stateless하게 설계한 이유

다중 서버 환경에서는 로드밸런서가 요청을 어떤 서버로 보낼지 알 수 없다.

따라서 특정 서버 메모리에 의존하는 구조는 확장에 불리하다.

예를 들어 API Server #1의 메모리에 주문 중복 요청 정보를 저장하면, 같은 요청이 API Server #2로 들어왔을 때 중복 여부를 알 수 없다.

그래서 중복 요청 방지, 포인트 잔액, 주문 상태는 모두 DB 기준으로 판단한다.

이 구조는 서버를 수평 확장하기 쉽다.

---

# 7. API 상세 설계

## 7.1 커피 메뉴 목록 조회 API

```http
GET /api/menus
```

### 설명

판매 중인 커피 메뉴 목록을 조회한다.

### Response

```json
{
  "menus": [
    {
      "menuId": 1,
      "name": "아메리카노",
      "price": 4000,
      "description": "진한 에스프레소 기반 커피"
    },
    {
      "menuId": 2,
      "name": "카페라떼",
      "price": 4500,
      "description": "우유가 들어간 부드러운 커피"
    }
  ]
}
```

### 설계 의도

메뉴 조회는 읽기 요청이 많고 데이터 변경은 상대적으로 적다.

따라서 DB 조회 결과를 Redis에 캐싱할 수 있다.

단, 주문 시에는 캐시된 가격을 그대로 사용하지 않고 DB에서 메뉴와 가격을 다시 조회한다.

메뉴 조회 API의 캐시는 화면 표시용이고, 결제 금액의 기준은 DB이다.

---

## 7.2 포인트 충전하기 API

```http
POST /api/points/charge
```

### Request

```json
{
  "requestId": "20260709-user1-charge-001",
  "amount": 10000
}
```

### Response

```json
{
  "userId": 1,
  "chargedAmount": 10000,
  "balance": 15000
}
```

### 처리 흐름

```text
1. requestId 중복 여부 확인
2. user_point row lock 획득
3. 포인트 잔액 증가
4. point_history 저장
5. transaction commit
```

### 설계 의도

포인트 충전도 중복 요청이 발생할 수 있다.

따라서 주문과 마찬가지로 `requestId`를 사용한다.

같은 충전 요청이 재전송되더라도 포인트가 두 번 증가하지 않도록 한다.

---

## 7.3 커피 주문, 결제하기 API

```http
POST /api/orders
```

### Request

```json
{
  "requestId": "20260709-user1-order-001",
  "items": [
    {
      "menuId": 1,
      "quantity": 2
    },
    {
      "menuId": 2,
      "quantity": 1
    }
  ]
}
```

### Response

```json
{
  "orderId": 100,
  "status": "PAID",
  "totalPrice": 12500,
  "balance": 2500,
  "items": [
    {
      "menuId": 1,
      "menuName": "아메리카노",
      "unitPrice": 4000,
      "quantity": 2,
      "linePrice": 8000
    },
    {
      "menuId": 2,
      "menuName": "카페라떼",
      "unitPrice": 4500,
      "quantity": 1,
      "linePrice": 4500
    }
  ]
}
```

### 처리 흐름

```text
1. requestId 중복 여부 확인
2. 주문 메뉴 목록 검증
3. 판매 중인 메뉴를 DB에서 조회
4. DB 기준 가격으로 총 주문 금액 계산
5. user_point row lock 획득
6. 잔액 검증
7. 포인트 차감
8. 주문 저장
9. 주문 상세 저장
10. 포인트 사용 이력 저장
11. order_outbox 저장
12. transaction commit
```

### 실패 케이스

| 상황 | 결과 |
|---|---|
| 존재하지 않는 메뉴 주문 | 주문 실패 |
| 판매 중지된 메뉴 주문 | 주문 실패 |
| 수량이 0 이하 | 주문 실패 |
| 포인트 부족 | 주문 실패 |
| 같은 requestId 재요청 | 기존 주문 결과 반환 |

### 설계 의도

주문과 결제는 분리하지 않고 하나의 API에서 처리한다.

이유는 이 시스템의 결제 수단이 외부 카드 결제가 아니라 내부 포인트이기 때문이다.

외부 PG 결제처럼 승인 대기, 결제 실패 callback, 비동기 승인 상태를 별도로 관리할 필요가 적다.

따라서 포인트 차감과 주문 생성을 하나의 트랜잭션으로 묶어 원자성을 보장한다.

---

## 7.4 인기 메뉴 목록 조회 API

```http
GET /api/menus/popular?period=DAILY&limit=10
```

### Response

```json
{
  "period": "DAILY",
  "menus": [
    {
      "menuId": 1,
      "name": "아메리카노",
      "price": 4000,
      "orderCount": 120,
      "totalQuantity": 180
    },
    {
      "menuId": 2,
      "name": "카페라떼",
      "price": 4500,
      "orderCount": 95,
      "totalQuantity": 130
    }
  ]
}
```

### 처리 흐름

```text
1. Redis에서 인기 메뉴 캐시 조회
2. cache hit이면 바로 반환
3. cache miss이면 menu_order_stat 조회
4. 메뉴 정보와 조합
5. Redis에 짧은 TTL로 저장
6. 응답 반환
```

### 설계 의도

인기 메뉴는 주문이 발생할 때마다 즉시 정확한 순위가 반영될 필요는 없다.

대신 빠른 조회 성능이 중요하다.

따라서 주문 원본 테이블을 직접 집계하지 않고 집계 테이블과 Redis 캐시를 사용한다.

---

# 8. 트랜잭션 설계

## 8.1 포인트 충전 트랜잭션

```text
@Transactional
chargePoint(userId, requestId, amount) {
    validatePositiveAmount(amount)
    validateIdempotency(userId, requestId)

    userPoint = userPointRepository.findByUserIdForUpdate(userId)
    userPoint.charge(amount)

    pointHistoryRepository.save(CHARGE history)
}
```

보장하는 것:

- 충전 금액이 한 번만 반영된다.
- 충전 이력이 반드시 남는다.
- 동시에 충전/주문 요청이 들어와도 최종 잔액이 깨지지 않는다.

---

## 8.2 주문 결제 트랜잭션

```text
@Transactional
order(userId, requestId, items) {
    validateIdempotency(userId, requestId)

    menus = menuRepository.findAllByIdAndStatus(items.menuIds, SELLING)
    totalPrice = calculateByDbMenuPrice(menus, items)

    userPoint = userPointRepository.findByUserIdForUpdate(userId)
    userPoint.use(totalPrice)

    order = orderRepository.save(PAID order)
    orderItemRepository.saveAll(snapshot order items)
    pointHistoryRepository.save(USE history)
    orderOutboxRepository.save(ORDER_PAID event)
}
```

보장하는 것:

- 포인트 차감과 주문 생성은 함께 성공하거나 함께 실패한다.
- 잔액 부족 시 주문은 생성되지 않는다.
- 주문 완료 이벤트는 outbox에 반드시 남는다.

---

# 9. 인덱스 및 제약 조건

## 9.1 Unique Constraint

```text
orders: unique(user_id, request_id)
point_history: unique(user_id, request_id, type)
```

목적:

- 주문 중복 처리 방지
- 포인트 충전 중복 처리 방지
- 다수 서버 환경에서도 동일하게 동작하는 중복 방어선 제공

---

## 9.2 Index

```text
menu: index(status)
orders: index(user_id, created_at)
order_item: index(menu_id)
menu_order_stat: index(stat_date, total_quantity)
menu_order_stat: unique(menu_id, stat_date)
order_outbox: index(status, created_at)
```

목적:

- 판매 중 메뉴 조회 최적화
- 사용자 주문 내역 조회 확장 대비
- 인기 메뉴 집계 조회 최적화
- outbox worker 처리 대상 조회 최적화

---

# 10. 동시성, 데이터 일관성, 확장성 관점 정리

## 10.1 동시성

| 문제 | 선택한 해결책 | 이유 |
|---|---|---|
| 동시에 여러 주문 요청 | `user_point` row 비관적 락 | 잔액 음수 방지 |
| 같은 주문 요청 재시도 | `requestId` + unique constraint | 중복 주문/중복 차감 방지 |
| 여러 API 서버에서 같은 요청 처리 | DB transaction, DB lock 사용 | 서버 메모리에 의존하지 않음 |
| 인기 메뉴 동시 집계 | 집계 테이블 unique key, outbox 재처리 | 이벤트 유실과 중복 처리 방지 |

---

## 10.2 데이터 일관성

| 데이터 | 일관성 수준 | 이유 |
|---|---|---|
| 포인트 잔액 | 강한 일관성 | 금전성 데이터 |
| 주문/결제 결과 | 강한 일관성 | 포인트 차감과 주문 생성의 원자성 필요 |
| 포인트 이력 | 강한 일관성 | 추적과 검증 필요 |
| 인기 메뉴 순위 | 최종적 일관성 | 약간 늦게 반영되어도 서비스 영향이 작음 |
| 메뉴 목록 캐시 | 최종적 일관성 | 화면 표시용이며 주문 시 DB 재검증 |

---

## 10.3 확장성

| 영역 | 확장 전략 |
|---|---|
| API 서버 | Stateless 구조로 수평 확장 |
| 메뉴 조회 | Redis 캐시 적용 |
| 인기 메뉴 조회 | 집계 테이블 + Redis 캐시 |
| 주문 이벤트 처리 | Outbox + Worker 구조 |
| 주문 데이터 증가 | 주문/주문상세 인덱스, 기간별 파티셔닝 고려 |
| 읽기 트래픽 증가 | 메뉴/인기 메뉴는 캐시, 주문 내역은 read replica 고려 |

---

# 11. 왜 이 구조가 설득력 있는가

이 설계는 모든 데이터를 같은 수준의 일관성으로 다루지 않는다.

대신 데이터의 성격에 따라 다른 전략을 선택한다.

- 포인트와 주문은 틀리면 안 되는 데이터이므로 DB 트랜잭션과 row lock을 사용한다.
- 인기 메뉴는 빠르게 읽히는 것이 중요하므로 집계와 캐시를 사용한다.
- 주문 성공 이벤트는 유실되면 안 되므로 outbox를 사용한다.
- API 서버는 상태를 가지지 않게 하여 다수 서버 환경에서도 동일하게 동작하도록 한다.

즉, 이 구조의 핵심은 다음과 같다.

```text
정확해야 하는 곳에는 강한 일관성
빠르게 조회해야 하는 곳에는 캐시와 집계
유실되면 안 되는 비동기 작업에는 outbox
서버 확장이 필요한 곳에는 stateless 구조
```

이렇게 설계하면 초기에는 단순한 커피 주문 시스템으로 시작할 수 있고, 트래픽이 증가해도 주문/결제의 안정성을 유지하면서 조회 성능을 확장할 수 있다.

---

# 12. 최종 결론

커피숍 주문 시스템에서 가장 중요한 것은 단순히 주문을 저장하는 것이 아니라, 여러 서버가 동시에 요청을 처리해도 포인트와 주문 데이터가 깨지지 않는 것이다.

따라서 이 설계는 포인트 결제 구간에는 RDBMS 트랜잭션과 비관적 락을 사용하여 강한 일관성을 보장한다.

반면 인기 메뉴 조회는 서비스 편의 기능에 가까우므로 집계 테이블과 Redis 캐시를 사용하여 성능과 확장성을 확보한다.

또한 주문 완료 후 인기 메뉴 집계가 누락되지 않도록 Transactional Outbox Pattern을 적용한다.

결과적으로 이 시스템은 다음을 만족한다.

- 다수 서버 환경에서도 안정적인 주문/결제 처리
- 포인트 중복 차감 및 잔액 음수 방지
- 주문 내역 기반 인기 메뉴 추천
- 조회 성능 향상을 위한 캐시/집계 구조
- 장애와 재시도를 고려한 이벤트 처리 구조
