package com.parksync.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // En producción: validar credenciales contra BD de operadores
        // Para el MVP: lógica simplificada demostrable en defensa
        String token = jwtService.generateToken(
                request.username(), "LOT-001", "ROLE_OPERATOR");
        return ResponseEntity.ok(new LoginResponse(token, 12 * 3600));
    }
}
