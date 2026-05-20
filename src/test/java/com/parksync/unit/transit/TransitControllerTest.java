package com.parksync.unit.transit;

import com.parksync.shared.ParkingStatus;
import com.parksync.transit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del TransitController — RTF-20, RTF-21.
 */
class TransitControllerTest {

    private TransitService transitService;
    private TransitController controller;

    @BeforeEach
    void setUp() {
        transitService = mock(TransitService.class);
        controller = new TransitController(transitService);
    }

    @Test
    void getGlobalStats_retorna_200_con_lista() {
        // RTF-20
        var stats = List.of(
                new ParkingStats("LOT-001", "Central", 50, 30, ParkingStatus.AMARILLO, "2026-04-17T14:30:00Z")
        );
        when(transitService.getGlobalStats()).thenReturn(stats);

        var response = controller.getGlobalStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("LOT-001", response.getBody().get(0).parkingLotId());
    }

    @Test
    void applyOverride_retorna_200_y_llama_servicio() {
        // RTF-21
        var req = new TransitOverrideRequest("LOT-001", ParkingStatus.ROJO, "Vía bloqueada");

        var response = controller.applyOverride(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transitService).applyOverride(req);
    }
}
