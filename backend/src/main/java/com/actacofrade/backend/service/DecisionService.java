package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateDecisionRequest;
import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.dto.UpdateDecisionRequest;
import com.actacofrade.backend.entity.Decision;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.HermandadArea;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.DecisionRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.AuthorizationHelper;
import com.actacofrade.backend.util.SanitizationUtils;
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
    private final AuditLogService auditLogService;
    private final AuthorizationHelper authorizationHelper;

    public DecisionService(DecisionRepository decisionRepository, EventRepository eventRepository,
                           UserRepository userRepository, AuditLogService auditLogService,
                           AuthorizationHelper authorizationHelper) {
        this.decisionRepository = decisionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.authorizationHelper = authorizationHelper;
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
        validateEventNotClosed(event);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        HermandadArea area = HermandadArea.valueOf(request.area());
        User reviewedBy = authorizationHelper.actsAsCollaboratorInEvent(currentUser, event)
                ? currentUser
                : resolveUser(request.reviewedById());
        authorizationHelper.requireAssignable(reviewedBy);

        Decision decision = new Decision();
        decision.setEvent(event);
        decision.setArea(area);
        decision.setTitle(SanitizationUtils.sanitize(request.title()));
        decision.setReviewedBy(reviewedBy);

        decisionRepository.save(decision);
        auditLogService.log(event, "DECISION", decision.getId(), "DECISION_CREATED", currentUser, decision.getTitle());
        return toResponse(decision);
    }

    public DecisionResponse update(Integer eventId, Integer decisionId, UpdateDecisionRequest request, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);

        AuditLogService.ChangeSetBuilder diff = new AuditLogService.ChangeSetBuilder();

        if (request.area() != null) {
            HermandadArea newArea = HermandadArea.valueOf(request.area());
            diff.track("area", decision.getArea(), newArea);
            decision.setArea(newArea);
        }
        if (request.title() != null) {
            String newTitle = SanitizationUtils.sanitize(request.title());
            diff.track("title", decision.getTitle(), newTitle);
            decision.setTitle(newTitle);
        }
        if (request.reviewedById() != null) {
            User newReviewer = resolveUser(request.reviewedById());
            authorizationHelper.requireAssignable(newReviewer);
            Integer oldId = decision.getReviewedBy() != null ? decision.getReviewedBy().getId() : null;
            Integer newId = newReviewer != null ? newReviewer.getId() : null;
            diff.track("reviewedById", oldId, newId);
            decision.setReviewedBy(newReviewer);
        }

        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        if (!diff.isEmpty()) {
            auditLogService.log(event, "DECISION", decision.getId(), "DECISION_UPDATED", currentUser, decision.getTitle(), diff.toJson());
        }
        return toResponse(decision);
    }

    public void delete(Integer eventId, Integer decisionId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Decision decision = findDecisionOrThrow(decisionId, eventId);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);
        decisionRepository.delete(decision);
    }

    public DecisionResponse accept(Integer eventId, Integer decisionId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        if (decision.getStatus() != DecisionStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden aceptar decisiones en estado PENDING");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);
        decision.setStatus(DecisionStatus.ACCEPTED);
        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        auditLogService.log(event, "DECISION", decision.getId(), "DECISION_ACCEPTED", currentUser, decision.getTitle());
        return toResponse(decision);
    }

    public DecisionResponse reject(Integer eventId, Integer decisionId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Decision decision = findDecisionOrThrow(decisionId, eventId);

        if (decision.getStatus() != DecisionStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden rechazar decisiones en estado PENDING");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);
        decision.setStatus(DecisionStatus.REJECTED);
        decision.setUpdatedAt(LocalDateTime.now());
        decisionRepository.save(decision);
        auditLogService.log(event, "DECISION", decision.getId(), "DECISION_REJECTED", currentUser, decision.getTitle());
        return toResponse(decision);
    }

    private Event findEventForHermandadOrThrow(Integer eventId, Integer hermandadId) {
        return eventRepository.findByIdAndHermandadId(eventId, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o no pertenece a tu hermandad"));
    }

    private void validateEventNotClosed(Event event) {
        if (event.getStatus() == EventStatus.CLOSED) {
            throw new IllegalStateException("El acto esta cerrado y no permite modificaciones");
        }
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
