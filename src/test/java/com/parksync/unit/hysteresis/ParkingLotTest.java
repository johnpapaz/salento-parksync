package com.parksync.unit.hysteresis;

import com.parksync.hysteresis.ParkingLot;
import com.parksync.shared.VehicleCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkingLotTest {

    @Test
    void testConstructorYGetters() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test Lot", 10, 20, 30);
        assertEquals("LOT-1", lot.getId());
        assertEquals("Test Lot", lot.getNombre());
        assertEquals(10, lot.getCapacidadMaximaCarros());
        assertEquals(20, lot.getCapacidadMaximaMotos());
        assertEquals(30, lot.getCapacidadMaximaBuses());
        assertEquals(0, lot.getOcupacionCarros());
        assertEquals(0, lot.getOcupacionMotos());
        assertEquals(0, lot.getOcupacionBuses());
    }

    @Test
    void testOcupacionPorcentajeConCapacidadCero() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 0, 0, 0);
        assertEquals(0.0, lot.getOcupacionPorcentaje());
    }

    @Test
    void testOcupacionPorcentaje() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 10, 10, 10);
        assertEquals(0.0, lot.getOcupacionPorcentaje());

        lot.incrementarCarros(); // 1
        lot.incrementarMotos();  // 1
        lot.incrementarBuses();  // 1
        // Total ocupación = 3, total capacidad = 30 -> 10%
        assertEquals(10.0, lot.getOcupacionPorcentaje());
    }

    @Test
    void testIncrementarYDecrementarCarros() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 2, 0, 0);

        // Incrementar
        lot.incrementarCarros();
        assertEquals(1, lot.getOcupacionCarros());
        lot.incrementarCarros();
        assertEquals(2, lot.getOcupacionCarros());

        // Incrementar por encima de la capacidad máxima no debe tener efecto
        lot.incrementarCarros();
        assertEquals(2, lot.getOcupacionCarros());

        // Decrementar
        lot.decrementarCarros();
        assertEquals(1, lot.getOcupacionCarros());
        lot.decrementarCarros();
        assertEquals(0, lot.getOcupacionCarros());

        // Decrementar por debajo de cero no debe tener efecto
        lot.decrementarCarros();
        assertEquals(0, lot.getOcupacionCarros());
    }

    @Test
    void testIncrementarYDecrementarMotos() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 0, 2, 0);

        // Incrementar
        lot.incrementarMotos();
        assertEquals(1, lot.getOcupacionMotos());
        lot.incrementarMotos();
        assertEquals(2, lot.getOcupacionMotos());

        // Limite
        lot.incrementarMotos();
        assertEquals(2, lot.getOcupacionMotos());

        // Decrementar
        lot.decrementarMotos();
        assertEquals(1, lot.getOcupacionMotos());
        lot.decrementarMotos();
        assertEquals(0, lot.getOcupacionMotos());

        // Limite
        lot.decrementarMotos();
        assertEquals(0, lot.getOcupacionMotos());
    }

    @Test
    void testIncrementarYDecrementarBuses() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 0, 0, 2);

        // Incrementar
        lot.incrementarBuses();
        assertEquals(1, lot.getOcupacionBuses());
        lot.incrementarBuses();
        assertEquals(2, lot.getOcupacionBuses());

        // Limite
        lot.incrementarBuses();
        assertEquals(2, lot.getOcupacionBuses());

        // Decrementar
        lot.decrementarBuses();
        assertEquals(1, lot.getOcupacionBuses());
        lot.decrementarBuses();
        assertEquals(0, lot.getOcupacionBuses());

        // Limite
        lot.decrementarBuses();
        assertEquals(0, lot.getOcupacionBuses());
    }

    @Test
    void testRecalibrar() {
        ParkingLot lot = new ParkingLot("LOT-1", "Test", 10, 10, 10);

        // Recalibrar PARTICULARES
        lot.recalibrar(VehicleCategory.PARTICULAR, 5);
        assertEquals(5, lot.getOcupacionCarros());

        // Recalibrar con valor negativo (debe limitar a 0)
        lot.recalibrar(VehicleCategory.PARTICULAR, -3);
        assertEquals(0, lot.getOcupacionCarros());

        // Recalibrar con valor superior a la capacidad (debe limitar al máximo)
        lot.recalibrar(VehicleCategory.PARTICULAR, 15);
        assertEquals(10, lot.getOcupacionCarros());

        // Recalibrar MOTOS
        lot.recalibrar(VehicleCategory.MOTOCICLETA, 8);
        assertEquals(8, lot.getOcupacionMotos());
        lot.recalibrar(VehicleCategory.MOTOCICLETA, -1);
        assertEquals(0, lot.getOcupacionMotos());
        lot.recalibrar(VehicleCategory.MOTOCICLETA, 20);
        assertEquals(10, lot.getOcupacionMotos());

        // Recalibrar BUSES
        lot.recalibrar(VehicleCategory.BUS, 3);
        assertEquals(3, lot.getOcupacionBuses());
        lot.recalibrar(VehicleCategory.BUS, -5);
        assertEquals(0, lot.getOcupacionBuses());
        lot.recalibrar(VehicleCategory.BUS, 12);
        assertEquals(10, lot.getOcupacionBuses());
    }
}
