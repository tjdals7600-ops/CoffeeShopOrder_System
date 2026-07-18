package com.example.coffeeshoporder_system.service;

// 주문 완료 이벤트를 데이터 수집 플랫폼으로 전송하는 연동 지점입니다.
public interface DataCollectionPlatformClient {


    void send(OrderPaidEvent event);
}
