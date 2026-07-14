package com.example.coffeeshoporder_system.service;

public interface DataCollectionPlatformClient {

    void send(OrderPaidEvent event);
}
