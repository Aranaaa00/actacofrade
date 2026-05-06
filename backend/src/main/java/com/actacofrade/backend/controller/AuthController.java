package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.HermandadOption;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.security.LoginRateLimiter;
import com.actacofrade.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro e inicio de sesión")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Operation(
            summary = "Registrar nuevo usuario y/o hermandad",
            description = "Registra un usuario. Si el rol es ADMINISTRADOR y la hermandad no existe, se crea junto al usuario. Devuelve un JWT inmediatamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "Registrado", value = "{\n  \"token\": \"eyJhbGciOiJIUzI1NiJ9...\",\n  \"email\": \"admin@hermandad.es\",\n  \"name\": \"Admin\",\n  \"roles\": [\"ADMINISTRADOR\"]\n}"))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(examples = @ExampleObject(value = "{\"status\":\"error\",\"message\":\"El email es obligatorio\",\"data\":null,\"errors\":[]}"))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(examples = @ExampleObject(value = "{\"status\":\"error\",\"message\":\"El email ya está registrado\",\"data\":null,\"errors\":[]}")))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar hermandades disponibles",
            description = "Devuelve la lista de hermandades existentes para que un usuario no administrador pueda seleccionar la suya en el registro.")
    @ApiResponse(responseCode = "200", description = "Lista de hermandades")
    @GetMapping("/hermandades")
    public ResponseEntity<List<HermandadOption>> listHermandades() {
        return ResponseEntity.ok(authService.listHermandades());
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica con email y contraseña y devuelve un JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cuerpo inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String clientKey = resolveClientKey(httpRequest, request.email());
        if (!loginRateLimiter.tryAcquire(clientKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de inicio de sesi\u00f3n. Int\u00e9ntalo de nuevo m\u00e1s tarde.");
        }
        AuthResponse response = authService.login(request);
        loginRateLimiter.recordSuccess(clientKey);
        return ResponseEntity.ok(response);
    }

    private String resolveClientKey(HttpServletRequest request, String email) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        return ip + "|" + normalizedEmail;
    }
}
