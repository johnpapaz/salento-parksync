package com.parksync.query;

import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.shared.ParkingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final ParkingLotRepository lotRepository;
    private final ParkingStateCache stateCache;

    public CacheWarmupRunner(ParkingLotRepository lotRepository, ParkingStateCache stateCache) {
        this.lotRepository = lotRepository;
        this.stateCache = stateCache;
    }

    @Override
    public void run(String... args) {
        log.info("[WARMUP] Iniciando precarga de IDs de parqueadero en Redis...");
        lotRepository.findAll().forEach(lot -> {
            stateCache.addActiveLot(lot.getId());
            // Si el estado no existe en Redis, lo inicializamos en GRIS para consistencia
            if (stateCache.getStatus(lot.getId()) == ParkingStatus.GRIS 
                    && stateCache.getLastUpdated(lot.getId()) == null) {
                stateCache.updateStatus(lot.getId(), ParkingStatus.GRIS);
            }
        });
        log.info("[WARMUP] Precarga completada.");
    }
}
