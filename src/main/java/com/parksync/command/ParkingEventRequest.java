package com.parksync.command;

import com.parksync.shared.EventType;
import com.parksync.shared.VehicleCategory;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** DTO de entrada desde el Service Worker del cliente (RTF-03, RTF-08). */
public record ParkingEventRequest(
    @NotNull UUID eventoId,
    @NotNull String parkingLotId,
    @NotNull Instant timestampOrigen,
    @NotNull EventType tipoEvento,
    VehicleCategory categoriaVehiculo,
    Integer valorAbsoluto               // requerido solo si tipoEvento = RECALIBRATION
) {}
