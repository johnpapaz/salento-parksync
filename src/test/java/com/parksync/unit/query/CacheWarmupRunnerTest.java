package com.parksync.unit.query;

import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.CacheWarmupRunner;
import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class CacheWarmupRunnerTest {

    private ParkingLotRepository lotRepository;
    private ParkingStateCache stateCache;
    private CacheWarmupRunner runner;

    @BeforeEach
    void setUp() {
        lotRepository = mock(ParkingLotRepository.class);
        stateCache = mock(ParkingStateCache.class);
        runner = new CacheWarmupRunner(lotRepository, stateCache);
    }

    @Test
    void testWarmupInitializationWithVariousStates() {
        ParkingLot lot1 = new ParkingLot("LOT-1", "Lot 1", 10, 10, 10);
        ParkingLot lot2 = new ParkingLot("LOT-2", "Lot 2", 10, 10, 10);
        ParkingLot lot3 = new ParkingLot("LOT-3", "Lot 3", 10, 10, 10);

        when(lotRepository.findAll()).thenReturn(List.of(lot1, lot2, lot3));

        // Case 1: LOT-1 has status != GRIS
        when(stateCache.getStatus("LOT-1")).thenReturn(ParkingStatus.VERDE);
        when(stateCache.getLastUpdated("LOT-1")).thenReturn("2026-05-21T09:00:00Z");

        // Case 2: LOT-2 has status == GRIS but lastUpdated is not null
        when(stateCache.getStatus("LOT-2")).thenReturn(ParkingStatus.GRIS);
        when(stateCache.getLastUpdated("LOT-2")).thenReturn("2026-05-21T09:00:00Z");

        // Case 3: LOT-3 has status == GRIS and lastUpdated is null (uninitialized)
        when(stateCache.getStatus("LOT-3")).thenReturn(ParkingStatus.GRIS);
        when(stateCache.getLastUpdated("LOT-3")).thenReturn(null);

        runner.run();

        // Verify active lots are added
        verify(stateCache).addActiveLot("LOT-1");
        verify(stateCache).addActiveLot("LOT-2");
        verify(stateCache).addActiveLot("LOT-3");

        // Verify updateStatus is only called for the uninitialized LOT-3
        verify(stateCache, never()).updateStatus(eq("LOT-1"), any());
        verify(stateCache, never()).updateStatus(eq("LOT-2"), any());
        verify(stateCache).updateStatus("LOT-3", ParkingStatus.GRIS);
    }
}
