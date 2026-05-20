package com.parksync.query;

import com.parksync.shared.ParkingStatus;

/**
 * DTO público para el turista — NUNCA incluye aforo numérico (CON-05, RTF-15).
 */
public record ParkingStatusResponse(
    String parkingLotId,
    ParkingStatus color,
    String lastUpdated      // RTF-16: para calcular desfase en el cliente
) {}
