package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.HermandadResponse;
import com.actacofrade.backend.dto.HermandadUpdateRequest;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class HermandadService {

    private final HermandadRepository hermandadRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public HermandadService(HermandadRepository hermandadRepository,
                            UserRepository userRepository,
                            EventRepository eventRepository) {
        this.hermandadRepository = hermandadRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public HermandadResponse getCurrent(String authenticatedEmail) {
        Hermandad hermandad = resolveHermandad(authenticatedEmail);
        return toResponse(hermandad);
    }

    public HermandadResponse updateCurrent(HermandadUpdateRequest request, String authenticatedEmail) {
        Hermandad hermandad = resolveHermandad(authenticatedEmail);

        String nombre = SanitizationUtils.sanitize(request.nombre());
        if (!hermandad.getNombre().equalsIgnoreCase(nombre)
                && hermandadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalStateException("Ya existe una hermandad con ese nombre");
        }

        hermandad.setNombre(nombre);
        hermandad.setDescripcion(SanitizationUtils.sanitizeNullable(request.descripcion()));
        hermandad.setAnioFundacion(request.anioFundacion());
        hermandad.setLocalidad(SanitizationUtils.sanitizeNullable(request.localidad()));
        hermandad.setDireccionSede(SanitizationUtils.sanitizeNullable(request.direccionSede()));
        hermandad.setEmailContacto(normalizeEmail(request.emailContacto()));
        hermandad.setTelefonoContacto(SanitizationUtils.sanitizeNullable(request.telefonoContacto()));
        hermandad.setSitioWeb(SanitizationUtils.sanitizeNullable(request.sitioWeb()));
        hermandad.setUpdatedAt(LocalDateTime.now());

        hermandadRepository.save(hermandad);
        return toResponse(hermandad);
    }

    private Hermandad resolveHermandad(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authenticatedEmail));
        Hermandad hermandad = user.getHermandad();
        if (hermandad == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return hermandad;
    }

    private String normalizeEmail(String email) {
        String sanitized = SanitizationUtils.sanitizeNullable(email);
        return sanitized == null ? null : sanitized.toLowerCase();
    }

    private HermandadResponse toResponse(Hermandad hermandad) {
        long usersCount = userRepository.countByHermandadIdAndActiveTrue(hermandad.getId());
        long eventsCount = eventRepository.countByHermandadId(hermandad.getId());
        return new HermandadResponse(
                hermandad.getId(),
                hermandad.getNombre(),
                hermandad.getDescripcion(),
                hermandad.getAnioFundacion(),
                hermandad.getLocalidad(),
                hermandad.getDireccionSede(),
                hermandad.getEmailContacto(),
                hermandad.getTelefonoContacto(),
                hermandad.getSitioWeb(),
                hermandad.getCreatedAt(),
                hermandad.getUpdatedAt(),
                usersCount,
                eventsCount
        );
    }
}
