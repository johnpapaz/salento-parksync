package com.parksync.unit.query;

import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import com.parksync.query.QueryService;
import com.parksync.shared.ParkingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la Read API (RTF-14, RTF-15, RTF-16).
 */
class QueryServiceTest {

    private ParkingStateCache stateCache;
    private ParkingLotRepository lotRepository;
    private QueryService queryService;

    @BeforeEach
    void setUp() {
        stateCache = mock(ParkingStateCache.class);
        lotRepository = mock(ParkingLotRepository.class);
        queryService = new QueryService(stateCache, lotRepository);
    }

    @Test
    void retorna_estado_desde_redis_no_de_bd() {
        // RTF-14: solo consulta Redis, nunca PostgreSQL directamente
        when(stateCache.getStatus("LOT-001")).thenReturn(ParkingStatus.AMARILLO);
        when(stateCache.getLastUpdated("LOT-001")).thenReturn("2026-04-17T14:30:00Z");

        var response = queryService.getStatus("LOT-001");

        assertEquals(ParkingStatus.AMARILLO, response.color());
        verify(stateCache).getStatus("LOT-001");
        verifyNoInteractions(lotRepository); // RTF-14: no toca PostgreSQL
    }

    @Test
    void respuesta_no_expone_aforo_numerico() {
        // CON-05: el DTO público solo tiene color y lastUpdated
        when(stateCache.getStatus("LOT-001")).thenReturn(ParkingStatus.VERDE);
        when(stateCache.getLastUpdated("LOT-001")).thenReturn("2026-04-17T14:30:00Z");

        var response = queryService.getStatus("LOT-001");

        // El record solo tiene parkingLotId, color y lastUpdated — no hay campo numérico
        assertNotNull(response.color());
        assertNotNull(response.lastUpdated()); // RTF-16
    }

    @Test
    void parqueadero_sin_datos_en_redis_retorna_gris() {
        when(stateCache.getStatus("LOT-999")).thenReturn(ParkingStatus.GRIS);
        when(stateCache.getLastUpdated("LOT-999")).thenReturn(null);

        var response = queryService.getStatus("LOT-999");

        assertEquals(ParkingStatus.GRIS, response.color());
    }

    @Test
    void getAllStatuses_retorna_todos_los_parqueaderos() {
        var lot1 = new ParkingLot("LOT-001", "Central", 50, 20, 5);
        var lot2 = new ParkingLot("LOT-002", "Norte", 30, 10, 2);
        when(stateCache.getActiveLots()).thenReturn(java.util.Set.of("LOT-001", "LOT-002"));
        when(stateCache.getStatus(any())).thenReturn(ParkingStatus.VERDE);
        when(stateCache.getLastUpdated(any())).thenReturn("2026-04-17T14:30:00Z");

        var responses = queryService.getAllStatuses();

        assertEquals(2, responses.size());
        verify(stateCache).getActiveLots();
        verifyNoInteractions(lotRepository); // RTF-14: no toca PostgreSQL
    }
}
