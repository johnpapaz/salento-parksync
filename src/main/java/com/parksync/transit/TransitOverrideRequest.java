package com.parksync.transit;

import com.parksync.shared.ParkingStatus;
import jakarta.validation.constraints.NotNull;

public record TransitOverrideRequest(
    @NotNull String parkingLotId,
    @NotNull ParkingStatus status,      // el agente puede forzar cualquier estado
    String motivo                        // campo libre para auditoría
) {}
