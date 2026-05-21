package com.parksync.unit.transit;

import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import com.parksync.transit.TransitOverrideRequest;
import com.parksync.transit.TransitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del Panel de Tránsito y override remoto (RTF-20, RTF-21).
 */
class TransitServiceTest {

    private ParkingLotRepository lotRepository;
    private ParkingStateCache stateCache;
    private TransitService transitService;

    @BeforeEach
    void setUp() {
        lotRepository = mock(ParkingLotRepository.class);
        stateCache = mock(ParkingStateCache.class);
        transitService = new TransitService(lotRepository, stateCache);
    }

    @Test
    void getGlobalStats_agrega_todos_los_parqueaderos_con_suma_total_de_aforos() {
        // RTF-20: panel global
        var lot = new ParkingLot("LOT-001", "Central", 50, 20, 5);
        lot.incrementarCarros(); // 1
        lot.incrementarMotos();  // 1
        lot.incrementarBuses();  // 1

        when(lotRepository.findAll()).thenReturn(List.of(lot));
        when(stateCache.getStatus("LOT-001")).thenReturn(ParkingStatus.VERDE);
        when(stateCache.getLastUpdated("LOT-001")).thenReturn("2026-04-17T14:30:00Z");

        var stats = transitService.getGlobalStats();

        assertEquals(1, stats.size());
        assertEquals("LOT-001", stats.get(0).parkingLotId());
        assertEquals(ParkingStatus.VERDE, stats.get(0).estado());
        // Capacidad total debe ser 50 + 20 + 5 = 75
        assertEquals(75, stats.get(0).capacidadTotal());
        // Ocupación actual debe ser 1 + 1 + 1 = 3
        assertEquals(3, stats.get(0).ocupacionActual());
    }

    @Test
    void override_remoto_actualiza_estado_en_redis() {
        // RTF-21: agente fuerza estado en cualquier parqueadero
        var req = new TransitOverrideRequest("LOT-001", ParkingStatus.ROJO, "Accidente en vía");

        transitService.applyOverride(req);

        verify(stateCache).updateStatus("LOT-001", ParkingStatus.ROJO);
    }

    @Test
    void override_puede_forzar_cualquier_estado() {
        // El agente puede forzar GRIS para indicar información no disponible
        var req = new TransitOverrideRequest("LOT-002", ParkingStatus.GRIS, "Fuera de servicio");

        transitService.applyOverride(req);

        verify(stateCache).updateStatus("LOT-002", ParkingStatus.GRIS);
    }
}
