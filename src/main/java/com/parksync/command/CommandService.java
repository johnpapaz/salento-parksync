package com.parksync.command;

import com.parksync.hysteresis.HysteresisEngine;
import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.shared.EventType;
import com.parksync.shared.VehicleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Procesa la cola de eventos del cliente.
 * Garantiza idempotencia (RTF-11) y orden FIFO por timestampOrigen (RTF-09).
 */
@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final ParkingEventRepository repository;
    private final ParkingLotRepository lotRepository;
    private final HysteresisEngine hysteresisEngine;

    public CommandService(ParkingEventRepository repository,
                          ParkingLotRepository lotRepository,
                          HysteresisEngine hysteresisEngine) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.hysteresisEngine = hysteresisEngine;
    }

    private static final int BATCH_SIZE_THRESHOLD = 50;

    @Transactional
    public void processBatch(List<ParkingEventRequest> requests) {
        if (requests.size() > BATCH_SIZE_THRESHOLD) {
            log.warn("[OVERLOAD] Lote de eventos demasiado grande: {}. Lanzando 503.", requests.size());
            throw new ServiceOverloadedException(30);
        }

        // RTF-09: orden cronológico FIFO
        requests.stream()
                .sorted(Comparator.comparing(ParkingEventRequest::timestampOrigen))
                .forEach(this::processOne);
    }

    private void processOne(ParkingEventRequest req) {
        String previousCorrelationId = org.slf4j.MDC.get("correlationId");
        org.slf4j.MDC.put("correlationId", req.eventoId().toString());
        try {
            // RTF-11: idempotencia — si el UUID ya existe, retornar 200 OK sin alterar estado
            if (repository.existsByEventoId(req.eventoId())) {
                log.info("[IDEMPOTENT] Evento {} ya procesado. Ignorado.", req.eventoId());
                return;
            }

            var event = new ParkingEvent(
                    req.eventoId(), req.parkingLotId(), req.timestampOrigen(),
                    req.tipoEvento(), req.categoriaVehiculo(), req.valorAbsoluto());
            repository.save(event);

            log.info("[EVENT] correlationId={} lot={} type={}", req.eventoId(), req.parkingLotId(), req.tipoEvento());

            // RTF-17: force_full bypassa el motor matemático
            if (req.tipoEvento() == EventType.FORCE_FULL) {
                hysteresisEngine.forceRed(req.parkingLotId());
                return;
            }

            // Actualizar aforo del parqueadero en PostgreSQL antes de recalcular
            lotRepository.findById(req.parkingLotId()).ifPresent(lot -> {
                updateOcupacion(lot, req);
                lotRepository.save(lot);
            });

            hysteresisEngine.recalculate(req.parkingLotId());
        } finally {
            if (previousCorrelationId != null) {
                org.slf4j.MDC.put("correlationId", previousCorrelationId);
            } else {
                org.slf4j.MDC.remove("correlationId");
            }
        }
    }

    /**
     * Aplica la mutación de ocupación según tipo de evento y categoría.
     * PRD §6.4 — segmentación estricta: cada categoría maneja su propia bolsa.
     */
    private void updateOcupacion(ParkingLot lot, ParkingEventRequest req) {
        VehicleCategory cat = req.categoriaVehiculo();

        switch (req.tipoEvento()) {
            case ENTRY -> {
                if (cat == VehicleCategory.PARTICULAR) lot.incrementarCarros();
                else if (cat == VehicleCategory.MOTOCICLETA) lot.incrementarMotos();
                else if (cat == VehicleCategory.BUS) lot.incrementarBuses();
            }
            case EXIT -> {
                if (cat == VehicleCategory.PARTICULAR) lot.decrementarCarros();
                else if (cat == VehicleCategory.MOTOCICLETA) lot.decrementarMotos();
                else if (cat == VehicleCategory.BUS) lot.decrementarBuses();
            }
            case RECALIBRATION -> {
                // RTF-07: set_absolute_value por categoría
                if (cat != null && req.valorAbsoluto() != null) {
                    lot.recalibrar(cat, req.valorAbsoluto());
                }
            }
            default -> { /* FORCE_FULL ya manejado arriba */ }
        }
    }
}
