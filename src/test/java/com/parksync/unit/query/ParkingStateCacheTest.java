package com.parksync.unit.query;

import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkingStateCacheTest {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private SetOperations<String, String> setOperations;
    private ParkingStateCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        cache = new ParkingStateCache(redisTemplate);
    }

    @Test
    void testUpdateStatus() {
        cache.updateStatus("LOT-1", ParkingStatus.AMARILLO);
        verify(hashOperations).putAll(eq("salento:park:LOT-1:current_state"), anyMap());
    }

    @Test
    void testGetStatusNormal() {
        when(hashOperations.get("salento:park:LOT-1:current_state", "color")).thenReturn("VERDE");
        assertEquals(ParkingStatus.VERDE, cache.getStatus("LOT-1"));
    }

    @Test
    void testGetStatusNull() {
        when(hashOperations.get("salento:park:LOT-1:current_state", "color")).thenReturn(null);
        assertEquals(ParkingStatus.GRIS, cache.getStatus("LOT-1"));
    }

    @Test
    void testGetStatusInvalidValue() {
        when(hashOperations.get("salento:park:LOT-1:current_state", "color")).thenReturn("INVALID_COLOR_NAME");
        assertEquals(ParkingStatus.GRIS, cache.getStatus("LOT-1"));
    }

    @Test
    void testGetLastUpdatedNormal() {
        String timestamp = "2026-05-21T09:00:00Z";
        when(hashOperations.get("salento:park:LOT-1:current_state", "last_updated")).thenReturn(timestamp);
        assertEquals(timestamp, cache.getLastUpdated("LOT-1"));
    }

    @Test
    void testGetLastUpdatedNull() {
        when(hashOperations.get("salento:park:LOT-1:current_state", "last_updated")).thenReturn(null);
        assertNull(cache.getLastUpdated("LOT-1"));
    }

    @Test
    void testAddActiveLot() {
        cache.addActiveLot("LOT-1");
        verify(setOperations).add("salento:park:active_lots", "LOT-1");
    }

    @Test
    void testGetActiveLots() {
        Set<String> lots = Set.of("LOT-1", "LOT-2");
        when(setOperations.members("salento:park:active_lots")).thenReturn(lots);
        assertEquals(lots, cache.getActiveLots());
    }
}
