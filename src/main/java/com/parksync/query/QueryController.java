package com.parksync.query;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read API — sirve estado exclusivamente desde Redis (RTF-14).
 * Endpoint público y anónimo para turistas (RTF-02, CON-04).
 * Ruta: GET /api/v1/queries/status/{parkingLotId}
 */
@RestController
@RequestMapping("/api/v1/queries")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/status/{parkingLotId}")
    public ResponseEntity<ParkingStatusResponse> getStatus(@PathVariable String parkingLotId) {
        return ResponseEntity.ok(queryService.getStatus(parkingLotId));
    }

    @GetMapping("/status")
    public ResponseEntity<List<ParkingStatusResponse>> getAllStatuses() {
        return ResponseEntity.ok(queryService.getAllStatuses());
    }
}
