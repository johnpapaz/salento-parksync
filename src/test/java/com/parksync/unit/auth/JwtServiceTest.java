package com.parksync.unit.auth;

import com.parksync.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del módulo JWT (RTF-01, NFR-10).
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inyectar secreto de prueba (en producción viene de variable de entorno)
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-at-least-32-chars-ok!");
    }

    @Test
    void token_generado_es_valido() {
        String token = jwtService.generateToken("operador1", "LOT-001", "ROLE_OPERATOR");
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void token_contiene_parking_lot_id_correcto() {
        String token = jwtService.generateToken("operador1", "LOT-002", "ROLE_OPERATOR");
        var claims = jwtService.validateAndExtract(token);
        assertEquals("LOT-002", claims.get("parkingLotId"));
    }

    @Test
    void token_contiene_username_correcto() {
        String token = jwtService.generateToken("juan", "LOT-001", "ROLE_OPERATOR");
        var claims = jwtService.validateAndExtract(token);
        assertEquals("juan", claims.getSubject());
    }

    @Test
    void token_manipulado_es_invalido() {
        String token = jwtService.generateToken("operador1", "LOT-001", "ROLE_OPERATOR");
        String tokenManipulado = token + "x";
        assertFalse(jwtService.isValid(tokenManipulado));
    }

    @Test
    void token_vacio_es_invalido() {
        assertFalse(jwtService.isValid(""));
    }
}
