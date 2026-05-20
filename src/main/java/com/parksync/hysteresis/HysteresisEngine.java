package com.parksync.hysteresis;

import com.parksync.query.ParkingStateCache;
import com.parksync.shared.ParkingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Motor de Histéresis — implementa RTF-13.
 *
 * Umbrales de ASCENSO:
 *   Verde  → Amarillo : ocupación >= 80 %
 *   Amarillo → Rojo   : ocupación >= 95 %
 *
 * Umbrales de DESCENSO (histéresis = margen anti-flapping):
 *   Rojo    → Amarillo : ocupación <= 90 %
 *   Amarillo → Verde   : ocupación <= 75 %
 *
 * Nunca se expone el número absoluto al turista (CON-05).
 */
@Service
public class HysteresisEngine {

    private static final Logger log = LoggerFactory.getLogger(HysteresisEngine.class);

    // Umbrales de ascenso
    static final double VERDE_A_AMARILLO   = 80.0;
    static final double AMARILLO_A_ROJO    = 95.0;

    // Umbrales de descenso (histéresis)
    static final double ROJO_A_AMARILLO    = 90.0;
    static final double AMARILLO_A_VERDE   = 75.0;

    private final ParkingLotRepository lotRepository;
    private final ParkingStateCache stateCache;

    public HysteresisEngine(ParkingLotRepository lotRepository, ParkingStateCache stateCache) {
        this.lotRepository = lotRepository;
        this.stateCache = stateCache;
    }

    /**
     * Recalcula el estado de un parqueadero y actualiza Redis.
     * Llamado tras cada evento normal (ENTRY / EXIT / RECALIBRATION).
     */
    public void recalculate(String parkingLotId) {
        lotRepository.findById(parkingLotId).ifPresent(lot -> {
            ParkingStatus currentStatus = stateCache.getStatus(parkingLotId);
            ParkingStatus newStatus = computeStatus(lot.getOcupacionPorcentaje(), currentStatus);

            if (newStatus != currentStatus) {
                log.info("[HYSTERESIS] lot={} {}% {} -> {}",
                        parkingLotId, String.format("%.1f", lot.getOcupacionPorcentaje()),
                        currentStatus, newStatus);
            }

            stateCache.updateStatus(parkingLotId, newStatus);
        });
    }

    /**
     * RTF-17: force_full — ignora matemática y fuerza Rojo directamente.
     */
    public void forceRed(String parkingLotId) {
        log.warn("[FORCE_FULL] lot={} forzado a ROJO por operador", parkingLotId);
        stateCache.updateStatus(parkingLotId, ParkingStatus.ROJO);
    }

    /**
     * Lógica pura de transición de estados con histéresis.
     * Separada del I/O para facilitar las pruebas unitarias.
     */
    public ParkingStatus computeStatus(double ocupacionPct, ParkingStatus estadoActual) {
        return switch (estadoActual) {
            case VERDE -> {
                if (ocupacionPct >= AMARILLO_A_ROJO) yield ParkingStatus.ROJO;
                if (ocupacionPct >= VERDE_A_AMARILLO) yield ParkingStatus.AMARILLO;
                yield ParkingStatus.VERDE;
            }
            case AMARILLO -> {
                if (ocupacionPct >= AMARILLO_A_ROJO) yield ParkingStatus.ROJO;
                if (ocupacionPct <= AMARILLO_A_VERDE) yield ParkingStatus.VERDE;
                yield ParkingStatus.AMARILLO;
            }
            case ROJO -> {
                if (ocupacionPct <= ROJO_A_AMARILLO) yield ParkingStatus.AMARILLO;
                yield ParkingStatus.ROJO;
            }
            case GRIS -> {
                // Tras Kill Switch: recalcula desde cero
                if (ocupacionPct >= AMARILLO_A_ROJO) yield ParkingStatus.ROJO;
                if (ocupacionPct >= VERDE_A_AMARILLO) yield ParkingStatus.AMARILLO;
                yield ParkingStatus.VERDE;
            }
        };
    }
}
