package com.parksync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parksync.command.ParkingEventRequest;
import com.parksync.hysteresis.ParkingLot;
import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.shared.EventType;
import com.parksync.shared.ParkingStatus;
import com.parksync.shared.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParkingLotRepository lotRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    private HashOperations<String, Object, Object> hashOperations;
    private final Map<String, Map<String, String>> mockRedisDb = new HashMap<>();

    @BeforeEach
    void setUp() {
        lotRepository.deleteAll();
        mockRedisDb.clear();

        // Configurar Mock Redis
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // Simular putAll en Redis
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Map<String, String> map = invocation.getArgument(1);
            mockRedisDb.put(key, map);
            return null;
        }).when(hashOperations).putAll(anyString(), anyMap());

        // Simular get en Redis
        when(hashOperations.get(anyString(), eq("color"))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Map<String, String> map = mockRedisDb.get(key);
            return map != null ? map.get("color") : null;
        });
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void testFullFlow_EventIngestion_To_Persistence_And_RedisUpdate() throws Exception {
        // 1. Crear el lote de parqueo en H2 (PostgreSQL compatible mode)
        // Capacidad: 10 carros, 10 motos, 10 buses (Total = 30)
        ParkingLot lot = new ParkingLot("LOT-999", "Parqueadero Test Integracion", 10, 10, 10);
        lotRepository.save(lot);

        // 2. Enviar evento de entrada de Carro (particular)
        UUID eventId1 = UUID.randomUUID();
        ParkingEventRequest event1 = new ParkingEventRequest(
                eventId1,
                "LOT-999",
                Instant.now(),
                EventType.ENTRY,
                VehicleCategory.PARTICULAR,
                null
        );

        mockMvc.perform(post("/api/v1/commands/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(event1))))
                .andExpect(status().isCreated());

        // Verificar persistencia en base de datos H2
        ParkingLot updatedLot = lotRepository.findById("LOT-999").orElseThrow();
        assertEquals(1, updatedLot.getOcupacionCarros());
        assertEquals(0, updatedLot.getOcupacionMotos());
        assertEquals(0, updatedLot.getOcupacionBuses());
        
        // Verificar que se actualizó Redis a VERDE (1 / 30 = 3.3% ocupación)
        String redisKey = "salento:park:LOT-999:current_state";
        assertNotNull(mockRedisDb.get(redisKey));
        assertEquals(ParkingStatus.VERDE.name(), mockRedisDb.get(redisKey).get("color"));

        // 3. Enviar eventos de Recalibración para alcanzar >= 80% (Amarillo)
        // Recalibremos: particulares = 9, motos = 9, buses = 7. Total = 25 de 30 (83.33%)
        UUID eventId2 = UUID.randomUUID();
        ParkingEventRequest event2 = new ParkingEventRequest(
                eventId2,
                "LOT-999",
                Instant.now().plusMillis(1),
                EventType.RECALIBRATION,
                VehicleCategory.PARTICULAR,
                9
        );
        UUID eventId3 = UUID.randomUUID();
        ParkingEventRequest event3 = new ParkingEventRequest(
                eventId3,
                "LOT-999",
                Instant.now().plusMillis(2),
                EventType.RECALIBRATION,
                VehicleCategory.MOTOCICLETA,
                9
        );
        UUID eventId4 = UUID.randomUUID();
        ParkingEventRequest event4 = new ParkingEventRequest(
                eventId4,
                "LOT-999",
                Instant.now().plusMillis(3),
                EventType.RECALIBRATION,
                VehicleCategory.BUS,
                7
        );

        // Enviamos el batch
        mockMvc.perform(post("/api/v1/commands/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(event2, event3, event4))))
                .andExpect(status().isCreated());

        updatedLot = lotRepository.findById("LOT-999").orElseThrow();
        assertEquals(9, updatedLot.getOcupacionCarros());
        assertEquals(9, updatedLot.getOcupacionMotos());
        assertEquals(7, updatedLot.getOcupacionBuses());
        
        // 25 / 30 = 83.33% -> Debe transicionar a AMARILLO (>= 80%)
        assertEquals(ParkingStatus.AMARILLO.name(), mockRedisDb.get(redisKey).get("color"));
    }
}
