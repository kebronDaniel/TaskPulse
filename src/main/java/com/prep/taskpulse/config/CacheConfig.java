package com.prep.taskpulse.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.security.service.TaskFlowUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    public static final String TASK_CACHE = "tasks";
    public static final String USERS_CACHE = "users";

    // Ignores computed UserDetails interface properties that have no matching
    // constructor param and cannot be set back after deserialization.
    @JsonIgnoreProperties({"authorities", "username", "password",
            "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    abstract static class TaskFlowUserDetailsMixin {}

    // Records are final, so NON_FINAL default typing skips them.
    // This mixin forces @class to be written so the type survives the Redis round-trip.
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    abstract static class TaskResponseMixin {}

    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer() {
        BasicPolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.prep.taskpulse")
                        .allowIfSubType("java.util")
                        .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.addMixIn(TaskFlowUserDetails.class, TaskFlowUserDetailsMixin.class);
        mapper.addMixIn(TaskResponse.class, TaskResponseMixin.class);
        mapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        RedisCacheConfiguration base =
                RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(redisSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(Map.of(
                        TASK_CACHE, base.entryTtl(Duration.ofMinutes(10)),
                        USERS_CACHE, base.entryTtl(Duration.ofMinutes(5))
                ))
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET failed for cache '{}', key '{}'. Falling back to DB.", cache.getName(), key, e);
            }
            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed for cache '{}', key '{}'.", cache.getName(), key, e);
            }
            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT failed for cache '{}', key '{}'.", cache.getName(), key, e);
            }
            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR failed for cache '{}'.", cache.getName(), e);
            }
        };
    }
}
