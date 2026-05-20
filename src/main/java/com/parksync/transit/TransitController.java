package com.parksync.transit;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Panel de Tránsito para agentes de la Secretaría (RTF-20, RTF-21).
 * Requiere rol TRANSIT_AGENT — protegido por JWT.
 */
@RestController
@RequestMapping("/api/v1/transit")
public class TransitController {

    private final TransitService transitService;

    public TransitController(TransitService transitService) {
        this.transitService = transitService;
    }

    /** RTF-20: panel global con capacidad y ocupación en tiempo real. */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('TRANSIT_AGENT')")
    public ResponseEntity<List<ParkingStats>> getGlobalStats() {
        return ResponseEntity.ok(transitService.getGlobalStats());
    }

    /** RTF-21: override manual remoto por agente de tránsito. */
    @PostMapping("/override")
    @PreAuthorize("hasRole('TRANSIT_AGENT')")
    public ResponseEntity<Void> applyOverride(@Valid @RequestBody TransitOverrideRequest request) {
        transitService.applyOverride(request);
        return ResponseEntity.ok().build();
    }
}
