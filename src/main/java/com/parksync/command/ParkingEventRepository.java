package com.parksync.command;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ParkingEventRepository extends JpaRepository<ParkingEvent, Long> {
    boolean existsByEventoId(UUID eventoId);
}
