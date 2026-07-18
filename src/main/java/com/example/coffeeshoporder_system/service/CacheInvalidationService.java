package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.config.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final CacheManager cacheManager;

    // 주문 집계가 커밋된 뒤에만 인기 메뉴 캐시를 비워 조회 결과의 일관성을 맞춥니다.
    public void evictPopularMenusAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictPopularMenus();
            return;
        }

        // 트랜잭션 안에서 호출되면 afterCommit hook으로 캐시 무효화를 지연합니다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictPopularMenus();
            }
        });
    }

    // 캐시 구현체가 Redis든 테스트용 simple cache든 동일하게 clear합니다.
    private void evictPopularMenus() {
        Cache cache = cacheManager.getCache(CacheNames.POPULAR_MENUS);
        if (cache != null) {
            cache.clear();
        }
    }
}
