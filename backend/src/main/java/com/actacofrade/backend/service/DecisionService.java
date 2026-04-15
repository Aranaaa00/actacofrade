package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateDecisionRequest;
import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.dto.UpdateDecisionRequest;
import com.actacofrade.backend.entity.Decision;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.HermandadArea;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.DecisionRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public DecisionService(DecisionRepository decisionRepository, EventRepository eventRepository,
                           UserRepository userRepository) {
        this.decisionRepository = decisionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public List<DecisionResponse> findByEventId(Integer eventId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        return decisionRepository.findByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DecisionResponse findById(Integer eventId, Integer decisionId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);
        return toResponse(decision);
    }

    public DecisionResponse create(Integer eventId, CreateDecisionRequest request, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        HermandadArea area = HermandadArea.valueOf(request.area());
        User reviewedBy = resolveUser(request.reviewedById());

        Decision decision = new Decision();
        decision.setEvent(event);
        decision.setArea(area);
        decision.setTitle(request.title());
        decision.setReviewedBy(reviewedBy);

        decisionRepository.save(decision);
        return toResponse(decision);
    }

    public DecisionResponse update(Integer eventId, Integer decisionId, UpdateDecisionRequest request, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        boolean isColaboradorOnly = currentUser.getRoles().stream()
                .allMatch(r -> r.getCode() == RoleCode.COLABORADOR);
        if (isColaboradorOnly) {
            if (decision.getReviewedBy() == null || !decision.getReviewedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Solo puedes editar las decisiones que tienes asignadas");
            }
        }

        if (request.area() != null) {
            decision.setArea(HermandadArea.valueOf(request.area()));
        }
        if (request.title() != null) {
            decision.setTitle(request.title());
        }
        if (request.reviewedById() != null) {
            decision.setReviewedBy(resolveUser(request.reviewedById()));
        }

        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        return toResponse(decision);
    }

    public void delete(Integer eventId, Integer decisionId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);
        decisionRepository.delete(decision);
    }

    public DecisionResponse markAsReady(Integer eventId, Integer decisionId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        if (decision.getStatus() == DecisionStatus.LISTA) {
            throw new IllegalStateException("La decision ya esta marcada como lista");
        }

        decision.setStatus(DecisionStatus.LISTA);
        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        return toResponse(decision);
    }

    public DecisionResponse reject(Integer eventId, Integer decisionId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        if (decision.getStatus() == DecisionStatus.RECHAZADA) {
            throw new IllegalStateException("La decision ya esta rechazada");
        }

        decision.setStatus(DecisionStatus.RECHAZADA);
        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        return toResponse(decision);
    }

    public DecisionResponse resetToPending(Integer eventId, Integer decisionId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        if (decision.getStatus() == DecisionStatus.PENDIENTE) {
            throw new IllegalStateException("La decision ya esta pendiente");
        }

        decision.setStatus(DecisionStatus.PENDIENTE);
        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        return toResponse(decision);
    }

    private Event findEventForHermandadOrThrow(Integer eventId, Integer hermandadId) {
        return eventRepository.findByIdAndHermandadId(eventId, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o no pertenece a tu hermandad"));
    }

    private Integer resolveHermandadId(String authenticatedEmail) {
        User user = findUserByEmailOrThrow(authenticatedEmail);
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return user.getHermandad().getId();
    }

    private Decision findDecisionOrThrow(Integer decisionId, Integer eventId) {
        return decisionRepository.findById(decisionId)
                .filter(decision -> decision.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new IllegalArgumentException("Decision no encontrada con id: " + decisionId + " en el acto: " + eventId));
    }

    private User resolveUser(Integer userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        }
        return user;
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + email));
    }

    private DecisionResponse toResponse(Decision decision) {
        Integer reviewedById = null;
        String reviewedByName = null;
        if (decision.getReviewedBy() != null) {
            reviewedById = decision.getReviewedBy().getId();
            reviewedByName = decision.getReviewedBy().getFullName();
        }

        return new DecisionResponse(
                decision.getId(),
                decision.getEvent().getId(),
                decision.getArea().name(),
                decision.getTitle(),
                decision.getStatus().name(),
                reviewedById,
                reviewedByName,
                decision.getCreatedAt(),
                decision.getUpdatedAt()
        );
    }
}
