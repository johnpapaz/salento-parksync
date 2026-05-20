package com.parksync.query;

import com.parksync.shared.ParkingStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstracción sobre Redis para el estado público de los parqueaderos.
 * Clave: salento:park:{id}:current_state
 * Valor: hash con campos "color" y "last_updated" (RTF-15, RTF-16).
 * Nunca se almacena aforo numérico (CON-05).
 */
@Component
public class ParkingStateCache {

    private static final String KEY_PREFIX = "salento:park:";
    private static final String KEY_SUFFIX = ":current_state";

    private final RedisTemplate<String, String> redisTemplate;

    public ParkingStateCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateStatus(String parkingLotId, ParkingStatus status) {
        String key = buildKey(parkingLotId);
        Map<String, String> fields = new HashMap<>();
        fields.put("color", status.name());
        fields.put("last_updated", Instant.now().toString());
        redisTemplate.opsForHash().putAll(key, fields);
    }

    public ParkingStatus getStatus(String parkingLotId) {
        Object val = redisTemplate.opsForHash().get(buildKey(parkingLotId), "color");
        if (val == null) return ParkingStatus.GRIS;
        try {
            return ParkingStatus.valueOf(val.toString());
        } catch (IllegalArgumentException e) {
            return ParkingStatus.GRIS;
        }
    }

    public String getLastUpdated(String parkingLotId) {
        Object val = redisTemplate.opsForHash().get(buildKey(parkingLotId), "last_updated");
        return val != null ? val.toString() : null;
    }

    private String buildKey(String parkingLotId) {
        return KEY_PREFIX + parkingLotId + KEY_SUFFIX;
    }
}
