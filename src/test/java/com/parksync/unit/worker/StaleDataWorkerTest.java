package com.parksync.unit.worker;

import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import com.parksync.worker.StaleDataWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Pruebas del Kill Switch (RTF-18, RTF-19).
 */
class StaleDataWorkerTest {

    private ParkingLotRepository lotRepository;
    private ParkingStateCache stateCache;
    private StaleDataWorker worker;

    @BeforeEach
    void setUp() {
        lotRepository = mock(ParkingLotRepository.class);
        stateCache = mock(ParkingStateCache.class);
        worker = new StaleDataWorker(lotRepository, stateCache);
    }

    @Test
    void parqueadero_sin_registro_en_redis_pasa_a_gris() {
        var lot = new ParkingLot("LOT-001", "Parqueadero Central", 50, 20, 5);
        when(lotRepository.findAll()).thenReturn(List.of(lot));
        when(stateCache.getLastUpdated("LOT-001")).thenReturn(null);

        worker.checkStaleData();

        verify(stateCache).updateStatus("LOT-001", ParkingStatus.GRIS);
    }

    @Test
    void parqueadero_con_desfase_mayor_30_min_pasa_a_gris() {
        // RTF-19: Kill Switch automático
        var lot = new ParkingLot("LOT-001", "Parqueadero Central", 50, 20, 5);
        when(lotRepository.findAll()).thenReturn(List.of(lot));

        String hace35min = Instant.now().minusSeconds(35 * 60).toString();
        when(stateCache.getLastUpdated("LOT-001")).thenReturn(hace35min);

        worker.checkStaleData();

        verify(stateCache).updateStatus("LOT-001", ParkingStatus.GRIS);
    }

    @Test
    void parqueadero_actualizado_recientemente_no_cambia() {
        var lot = new ParkingLot("LOT-001", "Parqueadero Central", 50, 20, 5);
        when(lotRepository.findAll()).thenReturn(List.of(lot));

        String hace5min = Instant.now().minusSeconds(5 * 60).toString();
        when(stateCache.getLastUpdated("LOT-001")).thenReturn(hace5min);

        worker.checkStaleData();

        verify(stateCache, never()).updateStatus(eq("LOT-001"), eq(ParkingStatus.GRIS));
    }

    @Test
    void evalua_todos_los_parqueaderos_en_cada_ciclo() {
        var lot1 = new ParkingLot("LOT-001", "Central", 50, 20, 5);
        var lot2 = new ParkingLot("LOT-002", "Norte", 30, 10, 2);
        when(lotRepository.findAll()).thenReturn(List.of(lot1, lot2));
        when(stateCache.getLastUpdated(any())).thenReturn(null);

        worker.checkStaleData();

        verify(stateCache).updateStatus("LOT-001", ParkingStatus.GRIS);
        verify(stateCache).updateStatus("LOT-002", ParkingStatus.GRIS);
    }
}
