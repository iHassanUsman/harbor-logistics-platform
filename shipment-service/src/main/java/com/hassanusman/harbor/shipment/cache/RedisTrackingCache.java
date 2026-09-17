package com.hassanusman.harbor.shipment.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.harbor.shipment.domain.Shipment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisTrackingCache implements TrackingCachePort {

    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisTrackingCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public void put(Shipment shipment) {
        try {
            redis.opsForValue().set("harbor:track:" + shipment.getId(), mapper.writeValueAsString(new TrackingView(
                    shipment.getId(),
                    shipment.getCustomerId(),
                    shipment.getStatus().name(),
                    shipment.getOrigin(),
                    shipment.getDestination(),
                    shipment.getRejectReason()
            )), TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<TrackingView> get(String shipmentId) {
        String json = redis.opsForValue().get("harbor:track:" + shipmentId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(json, TrackingView.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
