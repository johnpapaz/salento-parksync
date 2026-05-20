package com.parksync.transit;

import com.parksync.shared.ParkingStatus;

/** DTO del panel de Tránsito (RTF-20). */
public record ParkingStats(
    String parkingLotId,
    String nombre,
    int capacidadTotal,
    int ocupacionActual,
    ParkingStatus estado,
    String lastUpdated
) {}
