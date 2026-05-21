package com.parksync.unit.command;

import com.parksync.command.*;
import com.parksync.hysteresis.HysteresisEngine;
import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.shared.EventType;
import com.parksync.shared.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Pruebas de idempotencia y procesamiento FIFO (RTF-11, RTF-09).
 */
class CommandServiceTest {

    private ParkingEventRepository repository;
    private ParkingLotRepository lotRepository;
    private HysteresisEngine hysteresisEngine;
    private CommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(ParkingEventRepository.class);
        lotRepository = mock(ParkingLotRepository.class);
        hysteresisEngine = mock(HysteresisEngine.class);
        service = new CommandService(repository, lotRepository, hysteresisEngine);
    }

    @Test
    void evento_nuevo_se_guarda_y_dispara_recalculo() {
        UUID id = UUID.randomUUID();
        when(repository.existsByEventoId(id)).thenReturn(false);

        ParkingLot lot = new ParkingLot("LOT-001", "Salento Central", 10, 10, 10);
        when(lotRepository.findById("LOT-001")).thenReturn(Optional.of(lot));

        var req = new ParkingEventRequest(id, "LOT-001", Instant.now(),
                EventType.ENTRY, VehicleCategory.PARTICULAR, null);

        service.processBatch(List.of(req));

        verify(repository).save(any(ParkingEvent.class));
        verify(lotRepository).save(lot);
        verify(hysteresisEngine).recalculate("LOT-001");
    }

    @Test
    void evento_duplicado_no_se_guarda_ni_recalcula() {
        // RTF-11: idempotencia — mismo UUID llega dos veces
        UUID id = UUID.randomUUID();
        when(repository.existsByEventoId(id)).thenReturn(true);

        var req = new ParkingEventRequest(id, "LOT-001", Instant.now(),
                EventType.ENTRY, VehicleCategory.PARTICULAR, null);

        service.processBatch(List.of(req));

        verify(repository, never()).save(any());
        verify(lotRepository, never()).findById(any());
        verify(lotRepository, never()).save(any());
        verify(hysteresisEngine, never()).recalculate(any());
    }

    @Test
    void force_full_llama_forceRed_en_lugar_de_recalculate() {
        // RTF-17: force_full bypassa el motor matemático
        UUID id = UUID.randomUUID();
        when(repository.existsByEventoId(id)).thenReturn(false);

        var req = new ParkingEventRequest(id, "LOT-001", Instant.now(),
                EventType.FORCE_FULL, null, null);

        service.processBatch(List.of(req));

        verify(hysteresisEngine).forceRed("LOT-001");
        verify(lotRepository, never()).findById(any());
        verify(lotRepository, never()).save(any());
        verify(hysteresisEngine, never()).recalculate(any());
    }

    @Test
    void batch_de_dos_eventos_procesa_ambos() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(repository.existsByEventoId(any())).thenReturn(false);

        ParkingLot lot = new ParkingLot("LOT-001", "Salento Central", 10, 10, 10);
        when(lotRepository.findById("LOT-001")).thenReturn(Optional.of(lot));

        var req1 = new ParkingEventRequest(id1, "LOT-001", Instant.now().minusSeconds(10),
                EventType.ENTRY, VehicleCategory.PARTICULAR, null);
        var req2 = new ParkingEventRequest(id2, "LOT-001", Instant.now(),
                EventType.EXIT, VehicleCategory.PARTICULAR, null);

        service.processBatch(List.of(req2, req1)); // orden invertido a propósito

        verify(repository, times(2)).save(any());
        verify(lotRepository, times(2)).save(lot);
    }
}
