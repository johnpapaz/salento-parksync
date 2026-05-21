package com.parksync.hysteresis;

import jakarta.persistence.*;

/**
 * Master data del parqueadero (capacidades y estado operativo).
 * Los estados derivados viven en Redis, no aquí (CON-05).
 */
@Entity
@Table(name = "parking_lots")
public class ParkingLot {

    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int capacidadMaximaCarros;

    @Column(nullable = false)
    private int capacidadMaximaMotos;

    @Column(nullable = false)
    private int capacidadMaximaBuses;

    // Contadores actuales (fuente de verdad para el motor de histéresis)
    private int ocupacionCarros = 0;
    private int ocupacionMotos  = 0;
    private int ocupacionBuses  = 0;

    protected ParkingLot() {}

    public ParkingLot(String id, String nombre,
                      int capacidadCarros, int capacidadMotos, int capacidadBuses) {
        this.id = id;
        this.nombre = nombre;
        this.capacidadMaximaCarros = capacidadCarros;
        this.capacidadMaximaMotos  = capacidadMotos;
        this.capacidadMaximaBuses  = capacidadBuses;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getCapacidadMaximaCarros() { return capacidadMaximaCarros; }
    public int getCapacidadMaximaMotos()  { return capacidadMaximaMotos; }
    public int getCapacidadMaximaBuses()  { return capacidadMaximaBuses; }
    public int getOcupacionCarros() { return ocupacionCarros; }
    public int getOcupacionMotos()  { return ocupacionMotos; }
    public int getOcupacionBuses()  { return ocupacionBuses; }

    /**
     * Ocupación total del parqueadero sobre la capacidad máxima total.
     * RTF-13: insumo para el motor de histéresis.
     * Bolsas independientes por categoría — CON-05, PRD §6.4.
     */
    public double getOcupacionPorcentaje() {
        int capacidadTotal = capacidadMaximaCarros + capacidadMaximaMotos + capacidadMaximaBuses;
        if (capacidadTotal == 0) return 0;
        int ocupacionTotal = ocupacionCarros + ocupacionMotos + ocupacionBuses;
        return (double) ocupacionTotal / capacidadTotal * 100.0;
    }

    // ── Mutadores de Carros ────────────────────────────────────────────────

    public void incrementarCarros() {
        if (ocupacionCarros < capacidadMaximaCarros) ocupacionCarros++;
    }

    public void decrementarCarros() {
        if (ocupacionCarros > 0) ocupacionCarros--;
    }

    // ── Mutadores de Motos ────────────────────────────────────────────────

    public void incrementarMotos() {
        if (ocupacionMotos < capacidadMaximaMotos) ocupacionMotos++;
    }

    public void decrementarMotos() {
        if (ocupacionMotos > 0) ocupacionMotos--;
    }

    // ── Mutadores de Buses ────────────────────────────────────────────────

    public void incrementarBuses() {
        if (ocupacionBuses < capacidadMaximaBuses) ocupacionBuses++;
    }

    public void decrementarBuses() {
        if (ocupacionBuses > 0) ocupacionBuses--;
    }

    /**
     * RTF-07: set_absolute_value — recalibración por categoría de vehículo.
     * Evita que el operador corrija una bolsa equivocada al sincronizar offline.
     */
    public void recalibrar(com.parksync.shared.VehicleCategory categoria, int nuevaOcupacion) {
        switch (categoria) {
            case PARTICULAR  -> ocupacionCarros = Math.max(0, Math.min(nuevaOcupacion, capacidadMaximaCarros));
            case MOTOCICLETA -> ocupacionMotos  = Math.max(0, Math.min(nuevaOcupacion, capacidadMaximaMotos));
            case BUS         -> ocupacionBuses  = Math.max(0, Math.min(nuevaOcupacion, capacidadMaximaBuses));
        }
    }
}
