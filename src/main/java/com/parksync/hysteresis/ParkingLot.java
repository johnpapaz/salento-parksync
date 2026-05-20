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
    public int getOcupacionCarros() { return ocupacionCarros; }
    public int getOcupacionMotos()  { return ocupacionMotos; }
    public int getOcupacionBuses()  { return ocupacionBuses; }

    /** Ocupación total ponderada sobre capacidad de carros (entidad dominante). */
    public double getOcupacionPorcentaje() {
        if (capacidadMaximaCarros == 0) return 0;
        return (double) ocupacionCarros / capacidadMaximaCarros * 100.0;
    }

    public void incrementarCarros() {
        if (ocupacionCarros < capacidadMaximaCarros) ocupacionCarros++;
    }

    public void decrementarCarros() {
        if (ocupacionCarros > 0) ocupacionCarros--;
    }

    public void incrementarMotos() {
        if (ocupacionMotos < capacidadMaximaMotos) ocupacionMotos++;
    }

    public void decrementarMotos() {
        if (ocupacionMotos > 0) ocupacionMotos--;
    }

    public void recalibrar(int nuevaOcupacionCarros) {
        // RTF-07: set_absolute_value reinicia el contador
        this.ocupacionCarros = Math.max(0, Math.min(nuevaOcupacionCarros, capacidadMaximaCarros));
    }
}
