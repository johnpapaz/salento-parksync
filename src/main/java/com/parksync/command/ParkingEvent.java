package com.parksync.command;

import com.parksync.shared.EventType;
import com.parksync.shared.VehicleCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Event Store — registro append-only en PostgreSQL (RTF-12).
 * El campo eventoId tiene restricción UNIQUE para garantizar idempotencia (RTF-11).
 */
@Entity
@Table(name = "parking_events",
       indexes = @Index(name = "idx_parking_id_timestamp", columnList = "parkingLotId, timestampOrigen"))
public class ParkingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventoId;              // UUID v4 generado en el cliente (RTF-03)

    @Column(nullable = false)
    private String parkingLotId;

    @Column(nullable = false)
    private Instant timestampOrigen;    // timestamp del cliente en ISO 8601 (RTF-03)

    @Column(nullable = false)
    private Instant timestampServidor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType tipoEvento;

    @Enumerated(EnumType.STRING)
    private VehicleCategory categoriaVehiculo;  // null para force_full / recalibración

    private Integer valorAbsoluto;              // solo para RECALIBRATION (RTF-07)

    @Column(nullable = false)
    private String correlationId;               // = eventoId.toString() para trazabilidad (NFR-13)

    protected ParkingEvent() {}

    public ParkingEvent(UUID eventoId, String parkingLotId, Instant timestampOrigen,
                        EventType tipoEvento, VehicleCategory categoriaVehiculo,
                        Integer valorAbsoluto) {
        this.eventoId = eventoId;
        this.parkingLotId = parkingLotId;
        this.timestampOrigen = timestampOrigen;
        this.timestampServidor = Instant.now();
        this.tipoEvento = tipoEvento;
        this.categoriaVehiculo = categoriaVehiculo;
        this.valorAbsoluto = valorAbsoluto;
        this.correlationId = eventoId.toString();
    }

    public UUID getEventoId() { return eventoId; }
    public String getParkingLotId() { return parkingLotId; }
    public Instant getTimestampOrigen() { return timestampOrigen; }
    public EventType getTipoEvento() { return tipoEvento; }
    public VehicleCategory getCategoriaVehiculo() { return categoriaVehiculo; }
    public Integer getValorAbsoluto() { return valorAbsoluto; }
    public String getCorrelationId() { return correlationId; }
}
