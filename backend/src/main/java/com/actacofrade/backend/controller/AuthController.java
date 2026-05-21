package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.HermandadOption;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.dto.RegistrationStatusResponse;
import com.actacofrade.backend.dto.ResendVerificationRequest;
import com.actacofrade.backend.dto.ResetPasswordRequest;
import com.actacofrade.backend.security.LoginRateLimiter;
import com.actacofrade.backend.service.AuthService;
import com.actacofrade.backend.service.PasswordResetService;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Auth", description = "Registro, verificación de correo e inicio de sesión")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          LoginRateLimiter loginRateLimiter,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "Solicitar verificación de correo para un nuevo registro",
            description = "Valida los datos y envía un correo de verificación. No se crea ningún usuario en base de datos hasta que el correo es confirmado.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Correo de verificación enviado",
                    content = @Content(schema = @Schema(implementation = RegistrationStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Email o hermandad ya registrados")
    })
    @PostMapping("/register")
    public ResponseEntity<RegistrationStatusResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationStatusResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Verificar el correo y completar el registro",
            description = "Consume el token recibido por correo, crea el usuario en base de datos y devuelve el JWT inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificación correcta",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "404", description = "Token inválido o caducado"),
            @ApiResponse(responseCode = "409", description = "El correo ya fue registrado en otro flujo")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @RequestParam("token")
            @NotBlank(message = "El token es obligatorio")
            @Size(max = 256, message = "Token no válido")
            String token) {
        AuthResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reenviar correo de verificación",
            description = "Si existe una solicitud pendiente para ese correo, se envía un nuevo enlace. La respuesta es genérica para evitar enumeración.")
    @ApiResponse(responseCode = "202", description = "Solicitud aceptada")
    @PostMapping("/resend-verification")
    public ResponseEntity<RegistrationStatusResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        RegistrationStatusResponse response = authService.resendVerification(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Listar hermandades disponibles",
            description = "Devuelve la lista de hermandades existentes para que un usuario no administrador pueda seleccionar la suya en el registro.")
    @ApiResponse(responseCode = "200", description = "Lista de hermandades")
    @GetMapping("/hermandades")
    public ResponseEntity<List<HermandadOption>> listHermandades() {
        return ResponseEntity.ok(authService.listHermandades());
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica con email y contraseña y devuelve un JWT. Solo válido tras verificar el correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = "{\n  \"token\": \"eyJhbGciOiJIUzI1NiJ9...\",\n  \"email\": \"admin@hermandad.es\"\n}"))),
            @ApiResponse(responseCode = "400", description = "Cuerpo inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta sin verificar")
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

    @Operation(summary = "Restablecer contraseña con token",
            description = "Consume un token de restablecimiento emitido (por el SuperAdmin u otro flujo) y establece la nueva contraseña.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Token inválido, caducado o contraseña no permitida")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.consumeAndResetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "Solicitar restablecimiento de contrase\u00f1a (self-service)",
            description = "Emite un token de restablecimiento para el usuario autenticado y env\u00eda el correo de recuperaci\u00f3n. Requiere JWT v\u00e1lido.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Correo de recuperaci\u00f3n enviado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "500", description = "Fallo al enviar el correo")
    })
    @PostMapping("/me/password-reset")
    public ResponseEntity<Void> requestSelfPasswordReset(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticaci\u00f3n requerida");
        }
        try {
            authService.requestSelfPasswordReset(userDetails.getUsername());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
        return ResponseEntity.noContent().build();
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
