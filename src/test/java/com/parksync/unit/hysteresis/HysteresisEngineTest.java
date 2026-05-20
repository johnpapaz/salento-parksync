package com.parksync.unit.hysteresis;

import com.parksync.hysteresis.HysteresisEngine;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class HysteresisEngineTest {

    private HysteresisEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HysteresisEngine(
                mock(ParkingLotRepository.class),
                mock(ParkingStateCache.class));
    }

    // --- Ascenso desde VERDE ---

    @Test
    void verde_bajo_80_permanece_verde() {
        assertEquals(ParkingStatus.VERDE, engine.computeStatus(79.9, ParkingStatus.VERDE));
    }

    @Test
    void verde_en_80_cambia_a_amarillo() {
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(80.0, ParkingStatus.VERDE));
    }

    @Test
    void verde_en_95_salta_directo_a_rojo() {
        assertEquals(ParkingStatus.ROJO, engine.computeStatus(95.0, ParkingStatus.VERDE));
    }

    // --- Ascenso desde AMARILLO ---

    @Test
    void amarillo_bajo_95_permanece_amarillo() {
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(94.9, ParkingStatus.AMARILLO));
    }

    @Test
    void amarillo_en_95_cambia_a_rojo() {
        assertEquals(ParkingStatus.ROJO, engine.computeStatus(95.0, ParkingStatus.AMARILLO));
    }

    // --- Descenso desde ROJO (histéresis: necesita bajar hasta 90%) ---

    @Test
    void rojo_en_91_permanece_rojo() {
        assertEquals(ParkingStatus.ROJO, engine.computeStatus(91.0, ParkingStatus.ROJO));
    }

    @Test
    void rojo_en_90_cambia_a_amarillo() {
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(90.0, ParkingStatus.ROJO));
    }

    @Test
    void rojo_vaciado_solo_llega_a_amarillo_no_verde() {
        // El descenso desde ROJO pasa por AMARILLO primero — anti-flapping
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(0.0, ParkingStatus.ROJO));
    }

    // --- Descenso desde AMARILLO (histéresis: necesita bajar hasta 75%) ---

    @Test
    void amarillo_en_76_permanece_amarillo() {
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(76.0, ParkingStatus.AMARILLO));
    }

    @Test
    void amarillo_en_75_cambia_a_verde() {
        assertEquals(ParkingStatus.VERDE, engine.computeStatus(75.0, ParkingStatus.AMARILLO));
    }

    // --- Recuperación desde GRIS (post Kill Switch) ---

    @Test
    void gris_bajo_80_recalcula_a_verde() {
        assertEquals(ParkingStatus.VERDE, engine.computeStatus(50.0, ParkingStatus.GRIS));
    }

    @Test
    void gris_entre_80_y_95_recalcula_a_amarillo() {
        assertEquals(ParkingStatus.AMARILLO, engine.computeStatus(85.0, ParkingStatus.GRIS));
    }

    @Test
    void gris_en_95_recalcula_a_rojo() {
        assertEquals(ParkingStatus.ROJO, engine.computeStatus(95.0, ParkingStatus.GRIS));
    }
}
