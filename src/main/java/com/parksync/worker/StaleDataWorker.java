package com.parksync.worker;

import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Worker CRON que evalúa stale data cada 5 minutos (RTF-18).
 * Kill Switch: si un parqueadero no sincroniza en 30 min → estado GRIS (RTF-19).
 */
@Component
public class StaleDataWorker {

    private static final Logger log = LoggerFactory.getLogger(StaleDataWorker.class);
    private static final long STALE_THRESHOLD_MINUTES = 30;

    private final ParkingLotRepository lotRepository;
    private final ParkingStateCache stateCache;

    public StaleDataWorker(ParkingLotRepository lotRepository, ParkingStateCache stateCache) {
        this.lotRepository = lotRepository;
        this.stateCache = stateCache;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000) // cada 5 minutos (RTF-18)
    public void checkStaleData() {
        log.info("[STALE_WORKER] Iniciando evaluación de stale data...");

        lotRepository.findAll().forEach(lot -> {
            String lastUpdatedStr = stateCache.getLastUpdated(lot.getId());

            if (lastUpdatedStr == null) {
                log.warn("[STALE_WORKER] lot={} sin registro en Redis. Forzando GRIS.", lot.getId());
                stateCache.updateStatus(lot.getId(), ParkingStatus.GRIS);
                return;
            }

            Instant lastUpdated = Instant.parse(lastUpdatedStr);
            long minutesSinceUpdate = ChronoUnit.MINUTES.between(lastUpdated, Instant.now());

            if (minutesSinceUpdate > STALE_THRESHOLD_MINUTES) {
                log.warn("[KILL_SWITCH] lot={} desfase={}min > {}min. Activando GRIS.",
                        lot.getId(), minutesSinceUpdate, STALE_THRESHOLD_MINUTES);
                stateCache.updateStatus(lot.getId(), ParkingStatus.GRIS);
            }
        });

        log.info("[STALE_WORKER] Evaluación completada.");
    }
}
