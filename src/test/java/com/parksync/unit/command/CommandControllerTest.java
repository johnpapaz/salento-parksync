package com.parksync.unit.command;

import com.parksync.command.CommandController;
import com.parksync.command.CommandService;
import com.parksync.command.ParkingEventRequest;
import com.parksync.command.ServiceOverloadedException;
import com.parksync.shared.EventType;
import com.parksync.shared.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del CommandController — RTF-08, RTF-10.
 */
class CommandControllerTest {

    private CommandService commandService;
    private CommandController controller;

    @BeforeEach
    void setUp() {
        commandService = mock(CommandService.class);
        controller = new CommandController(commandService);
    }

    @Test
    void batch_valido_retorna_201_created() {
        var req = new ParkingEventRequest(UUID.randomUUID(), "LOT-001", Instant.now(),
                EventType.ENTRY, VehicleCategory.PARTICULAR, null);

        var response = controller.receiveEvents(List.of(req));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(commandService).processBatch(anyList());
    }

    @Test
    void cuando_servicio_sobrecargado_retorna_503_con_retry_after() {
        // RTF-10: HTTP 503 + header Retry-After
        doThrow(new ServiceOverloadedException(30))
                .when(commandService).processBatch(anyList());

        var req = new ParkingEventRequest(UUID.randomUUID(), "LOT-001", Instant.now(),
                EventType.ENTRY, VehicleCategory.PARTICULAR, null);

        var response = controller.receiveEvents(List.of(req));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("30", response.getHeaders().getFirst("Retry-After"));
    }
}
