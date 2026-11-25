package com.qrorder.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qrorder.catalog.api.dto.MenuItemResponse;
import com.qrorder.catalog.api.dto.TableContextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis & Caching Configuration (Topik 1, 2, 40).
 * Uses typed Jackson2JsonRedisSerializer for Java records/immutable lists
 * and a CacheErrorHandler so Redis failures degrade gracefully to PostgreSQL (PRD §13.2).
 */
@Configuration
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static ObjectMapper createCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static Jackson2JsonRedisSerializer<List<MenuItemResponse>> activeMenuSerializer(ObjectMapper cacheMapper) {
        var menuListType = cacheMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, MenuItemResponse.class);
        return new Jackson2JsonRedisSerializer<>(cacheMapper, menuListType);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(createCacheObjectMapper());

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            log.info("Initializing RedisCacheManager with typed JSON serialization and dynamic TTLs");

            ObjectMapper cacheMapper = createCacheObjectMapper();
            StringRedisSerializer keySerializer = new StringRedisSerializer();
            var activeMenuSerializer = activeMenuSerializer(cacheMapper);
            var outletSerializer = new Jackson2JsonRedisSerializer<>(cacheMapper, TableContextResponse.class);

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .computePrefixWith(cacheName -> "qrorder:v2:" + cacheName + ":")
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)))
                    .disableCachingNullValues();

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            // Topik 40: Specific TTL for active menu (10 min) and outlet details (1 hour)
            cacheConfigurations.put("activeMenu", defaultConfig
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(activeMenuSerializer)));
            cacheConfigurations.put("outletDetails", defaultConfig
                    .entryTtl(Duration.ofHours(1))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(outletSerializer)));

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();
        } catch (Exception e) {
            log.warn("Redis unavailable for cache manager, falling back to ConcurrentMapCacheManager: {}", e.getMessage());
            return new ConcurrentMapCacheManager("activeMenu", "outletDetails");
        }
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET error [cache={}, key={}]: {}. Degrading gracefully to database.",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT error [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT error [cache={}, key={}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR error [cache={}]: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
