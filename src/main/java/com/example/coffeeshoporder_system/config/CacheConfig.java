package com.example.coffeeshoporder_system.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@EnableCaching
@Configuration
public class CacheConfig {

    // 운영 프로필에서는 Redis를 캐시 저장소로 사용합니다.
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisCacheConfiguration defaultConfig = createRedisCacheConfiguration(objectMapper, Duration.ofMinutes(5));

        // 메뉴 목록은 비교적 천천히 변하고, 인기 메뉴는 주문 이후 빠르게 갱신되어야 합니다.
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheNames.SELLING_MENUS, createRedisCacheConfiguration(objectMapper, Duration.ofMinutes(10)),
                CacheNames.POPULAR_MENUS, createRedisCacheConfiguration(objectMapper, Duration.ofMinutes(1))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    // Redis에 저장되는 key/value 직렬화 방식과 TTL 정책을 한 곳에서 맞춥니다.
    private RedisCacheConfiguration createRedisCacheConfiguration(ObjectMapper objectMapper, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJacksonJsonRedisSerializer(objectMapper)
                ));
    }
}
