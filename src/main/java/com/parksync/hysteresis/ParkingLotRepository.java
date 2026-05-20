package com.parksync.hysteresis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {
    List<ParkingLot> findAll();
}
