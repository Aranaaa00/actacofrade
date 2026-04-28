package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.HermandadResponse;
import com.actacofrade.backend.dto.HermandadUpdateRequest;
import com.actacofrade.backend.service.HermandadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hermandades")
public class HermandadController {

    private final HermandadService hermandadService;

    public HermandadController(HermandadService hermandadService) {
        this.hermandadService = hermandadService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA')")
    public ResponseEntity<HermandadResponse> getCurrent(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(hermandadService.getCurrent(userDetails.getUsername()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<HermandadResponse> updateCurrent(@Valid @RequestBody HermandadUpdateRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(hermandadService.updateCurrent(request, userDetails.getUsername()));
    }
}
