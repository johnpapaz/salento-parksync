package com.parksync.transit;

import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransitService {

    private static final Logger log = LoggerFactory.getLogger(TransitService.class);

    private final ParkingLotRepository lotRepository;
    private final ParkingStateCache stateCache;

    public TransitService(ParkingLotRepository lotRepository, ParkingStateCache stateCache) {
        this.lotRepository = lotRepository;
        this.stateCache = stateCache;
    }

    /** RTF-20: agrega capacidad y ocupación en tiempo real de todos los parqueaderos. */
    public List<ParkingStats> getGlobalStats() {
        return lotRepository.findAll().stream()
                .map(lot -> {
                    int capacidadTotal = lot.getCapacidadMaximaCarros() + lot.getCapacidadMaximaMotos() + lot.getCapacidadMaximaBuses();
                    int ocupacionTotal = lot.getOcupacionCarros() + lot.getOcupacionMotos() + lot.getOcupacionBuses();
                    return new ParkingStats(
                            lot.getId(),
                            lot.getNombre(),
                            capacidadTotal,
                            ocupacionTotal,
                            stateCache.getStatus(lot.getId()),
                            stateCache.getLastUpdated(lot.getId()));
                })
                .toList();
    }

    /** RTF-21: override remoto — un agente fuerza el estado de cualquier parqueadero. */
    public void applyOverride(TransitOverrideRequest req) {
        log.warn("[TRANSIT_OVERRIDE] lot={} status={} motivo={}",
                req.parkingLotId(), req.status(), req.motivo());
        stateCache.updateStatus(req.parkingLotId(), req.status());
    }
}
