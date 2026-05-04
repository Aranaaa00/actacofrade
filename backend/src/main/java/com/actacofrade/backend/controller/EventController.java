package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateEventRequest;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.dto.UpdateEventRequest;
import com.actacofrade.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Gestión de actos cofrades")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Listar todos los actos visibles para el usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de actos",
            content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = EventResponse.class))))
    @GetMapping
    public ResponseEntity<List<EventResponse>> findAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findAll(userDetails.getUsername()));
    }

    @Operation(summary = "Filtrar y paginar actos abiertos",
            description = "Devuelve actos no cerrados aplicando los filtros opcionales indicados.")
    @ApiResponse(responseCode = "200", description = "Página de actos")
    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> findFiltered(
            @Parameter(description = "Tipo de acto (enum EventType)") @RequestParam(required = false) String eventType,
            @Parameter(description = "Estado del acto (enum EventStatus)") @RequestParam(required = false) String status,
            @Parameter(description = "Fecha exacta del acto (ISO yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @Parameter(description = "Búsqueda por título") @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findFiltered(eventType, status, eventDate, search, pageable, userDetails.getUsername()));
    }

    @Operation(summary = "Histórico de actos cerrados",
            description = "Devuelve actos en estado CERRADO con paginación clásica (page/size) y filtros.")
    @GetMapping("/history")
    public ResponseEntity<Page<EventResponse>> findHistory(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer responsibleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findHistory(eventType, responsibleId, dateFrom, dateTo, search, page, size, userDetails.getUsername()));
    }

    @Operation(summary = "Fechas con actos programados", description = "Devuelve la lista de fechas (ISO) con al menos un acto.")
    @GetMapping("/available-dates")
    public ResponseEntity<List<String>> getAvailableDates(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getAvailableDates(userDetails.getUsername()));
    }

    @Operation(summary = "Obtener un acto por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acto encontrado",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "404", description = "Acto no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable Integer id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findById(id, userDetails.getUsername()));
    }

    @Operation(summary = "Crear un nuevo acto",
            description = "Solo accesible para ADMINISTRADOR o RESPONSABLE.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = CreateEventRequest.class),
                            examples = @ExampleObject(value = "{\n  \"title\": \"Salida procesional\",\n  \"eventType\": \"PROCESION\",\n  \"eventDate\": \"2026-04-03\",\n  \"description\": \"Estación de penitencia\"\n}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Acto creado",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, userDetails.getUsername()));
    }

    @Operation(summary = "Actualizar un acto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Acto no existe")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> update(@PathVariable Integer id,
                                                @Valid @RequestBody UpdateEventRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.update(id, request, userDetails.getUsername()));
    }

    @Operation(summary = "Eliminar un acto", description = "Solo ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Acto eliminado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Acto no existe")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        eventService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Avanzar al siguiente estado del flujo del acto")
    @PatchMapping("/{id}/advance-status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> advanceStatus(@PathVariable Integer id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.advanceStatus(id, userDetails.getUsername()));
    }

    @Operation(summary = "Cerrar definitivamente un acto")
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> close(@PathVariable Integer id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.close(id, userDetails.getUsername()));
    }

    @Operation(summary = "Bloquear/desbloquear un acto para cierre")
    @PatchMapping("/{id}/toggle-lock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> toggleLock(@PathVariable Integer id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.toggleLockForClosing(id, userDetails.getUsername()));
    }

    @Operation(summary = "Clonar un acto existente como nuevo acto en planificación")
    @ApiResponse(responseCode = "201", description = "Acto clonado")
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> cloneEvent(@PathVariable Integer id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.clone(id, userDetails.getUsername()));
    }
}
