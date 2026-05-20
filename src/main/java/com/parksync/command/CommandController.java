package com.parksync.command;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Write API — recibe la cola de eventos desde el Service Worker (RTF-08).
 * Ruta: POST /api/v1/commands/events
 *
 * RTF-10: en caso de sobrecarga retorna HTTP 503 + Retry-After para que
 * el cliente aplique Exponential Backoff sin saturar el servidor.
 */
@RestController
@RequestMapping("/api/v1/commands")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvents(@Valid @RequestBody List<ParkingEventRequest> events) {
        try {
            commandService.processBatch(events);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (ServiceOverloadedException e) {
            // RTF-10: señalizar al Service Worker que reintente con backoff
            HttpHeaders headers = new HttpHeaders();
            headers.set("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).build();
        }
    }
}
