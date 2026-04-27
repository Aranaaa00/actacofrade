package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.MyTaskResponse;
import com.actacofrade.backend.dto.MyTaskStatsResponse;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.EventType;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.AuthorizationHelper;
import com.actacofrade.backend.util.SanitizationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuthorizationHelper authorizationHelper;

    public TaskService(TaskRepository taskRepository, EventRepository eventRepository,
                       UserRepository userRepository, AuditLogService auditLogService,
                       AuthorizationHelper authorizationHelper) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.authorizationHelper = authorizationHelper;
    }

    public List<TaskResponse> findByEventId(Integer eventId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        return taskRepository.findByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse findById(Integer eventId, Integer taskId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Task task = findTaskOrThrow(taskId, eventId);
        return toResponse(task);
    }

    public TaskResponse create(Integer eventId, CreateTaskRequest request, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);

        User assignedTo = authorizationHelper.isColaboradorOnly(currentUser)
                ? currentUser
                : resolveUser(request.assignedToId());

        Task task = new Task();
        task.setEvent(event);
        task.setTitle(SanitizationUtils.sanitize(request.title()));
        task.setDescription(SanitizationUtils.sanitizeNullable(request.description()));
        task.setAssignedTo(assignedTo);
        task.setCreatedBy(currentUser);
        task.setDeadline(request.deadline());

        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_CREATED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse update(Integer eventId, Integer taskId, UpdateTaskRequest request, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (authorizationHelper.isColaboradorOnly(currentUser)) {
            Integer creatorId = task.getCreatedBy() != null ? task.getCreatedBy().getId() : null;
            if (!authorizationHelper.isOwner(currentUser.getId(), creatorId)) {
                throw new AccessDeniedException("Solo puedes editar las tareas que has creado");
            }
            if (request.assignedToId() != null && !request.assignedToId().equals(currentUser.getId())) {
                throw new AccessDeniedException("No puedes asignar esta tarea a otro usuario");
            }
        } else {
            authorizationHelper.requireEventManager(event, currentUser);
        }

        if (request.title() != null) {
            task.setTitle(SanitizationUtils.sanitize(request.title()));
        }
        if (request.description() != null) {
            task.setDescription(SanitizationUtils.sanitize(request.description()));
        }
        if (request.assignedToId() != null) {
            task.setAssignedTo(resolveUser(request.assignedToId()));
        }
        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }

        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_UPDATED", currentUser, task.getTitle());
        return toResponse(task);
    }

    public void delete(Integer eventId, Integer taskId, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);
        authorizationHelper.requireEventManager(event, currentUser);
        String taskTitle = task.getTitle();
        taskRepository.delete(task);
        auditLogService.log(event, "TASK", taskId, "TASK_DELETED", currentUser, taskTitle);
        recalculateEventProgress(event);
    }

    public TaskResponse accept(Integer eventId, Integer taskId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() != TaskStatus.PLANNED) {
            throw new IllegalStateException("Solo se pueden aceptar tareas en estado PLANNED");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);

        task.setStatus(TaskStatus.ACCEPTED);
        task.setRejectionReason(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_ACCEPTED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse startPreparation(Integer eventId, Integer taskId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() != TaskStatus.ACCEPTED) {
            throw new IllegalStateException("Solo se pueden preparar tareas en estado ACCEPTED");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireTaskAssigned(task, currentUser);

        task.setStatus(TaskStatus.IN_PREPARATION);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_IN_PREPARATION", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse confirm(Integer eventId, Integer taskId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() != TaskStatus.IN_PREPARATION) {
            throw new IllegalStateException("Solo se pueden confirmar tareas en estado IN_PREPARATION");
        }

        User confirmer = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireTaskAssigned(task, confirmer);

        task.setStatus(TaskStatus.CONFIRMED);
        task.setConfirmedBy(confirmer);
        task.setConfirmedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_CONFIRMED", confirmer, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse complete(Integer eventId, Integer taskId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() != TaskStatus.CONFIRMED) {
            throw new IllegalStateException("Solo se pueden completar tareas en estado CONFIRMED");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireTaskAssigned(task, currentUser);

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_COMPLETED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse reject(Integer eventId, Integer taskId, String rejectionReason, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.REJECTED) {
            throw new IllegalStateException("La tarea ya esta rechazada");
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("No se puede rechazar una tarea completada");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalStateException("El motivo de rechazo es obligatorio");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);

        task.setStatus(TaskStatus.REJECTED);
        task.setRejectionReason(SanitizationUtils.sanitize(rejectionReason));
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_REJECTED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse resetToPlanned(Integer eventId, Integer taskId, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.PLANNED) {
            throw new IllegalStateException("La tarea ya esta en estado PLANNED");
        }

        authorizationHelper.requireEventManager(event, currentUser);

        task.setStatus(TaskStatus.PLANNED);
        task.setRejectionReason(null);
        task.setConfirmedBy(null);
        task.setConfirmedAt(null);
        task.setCompletedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_RESET", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<MyTaskResponse> findMyTasks(String authenticatedEmail, String eventType,
                                            String statusGroup, String search, Pageable pageable) {
        User user = findUserByEmailOrThrow(authenticatedEmail);
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Integer userId = user.getId();
        Integer hermandadId = user.getHermandad().getId();

        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("assignedTo").get("id"), userId));
            predicates.add(cb.equal(root.get("event").get("hermandad").get("id"), hermandadId));

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("event").get("eventType"), EventType.valueOf(eventType)));
            }

            if (statusGroup != null && !statusGroup.isBlank()) {
                List<TaskStatus> statuses = resolveStatusGroup(statusGroup);
                predicates.add(root.get("status").in(statuses));
            }

            if (search != null && !search.isBlank()) {
                String sanitized = SanitizationUtils.sanitize(search).toLowerCase();
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + sanitized + "%"));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(
                        cb.asc(cb.selectCase()
                                .when(cb.equal(root.get("status"), TaskStatus.REJECTED), 1)
                                .otherwise(0)),
                        cb.asc(cb.coalesce(root.get("deadline"), cb.literal(LocalDate.of(9999, 12, 31)))),
                        cb.desc(root.get("createdAt"))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable).map(this::toMyTaskResponse);
    }

    @Transactional(readOnly = true)
    public MyTaskStatsResponse getMyTaskStats(String authenticatedEmail) {
        User user = findUserByEmailOrThrow(authenticatedEmail);
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Integer userId = user.getId();
        Integer hermandadId = user.getHermandad().getId();

        long pendingCount = taskRepository.countByAssignedToIdAndStatusAndEventHermandadId(
                userId, TaskStatus.PLANNED, hermandadId);
        long confirmedCount = taskRepository.countByAssignedToIdAndStatusInAndEventHermandadId(
                userId, List.of(TaskStatus.ACCEPTED, TaskStatus.IN_PREPARATION, TaskStatus.CONFIRMED, TaskStatus.COMPLETED), hermandadId);
        long rejectedCount = taskRepository.countByAssignedToIdAndStatusAndEventHermandadId(
                userId, TaskStatus.REJECTED, hermandadId);

        return new MyTaskStatsResponse(pendingCount, confirmedCount, rejectedCount);
    }

    private List<TaskStatus> resolveStatusGroup(String statusGroup) {
        return switch (statusGroup) {
            case "PENDING" -> List.of(TaskStatus.PLANNED);
            case "CONFIRMED" -> List.of(TaskStatus.ACCEPTED, TaskStatus.IN_PREPARATION, TaskStatus.CONFIRMED, TaskStatus.COMPLETED);
            case "REJECTED" -> List.of(TaskStatus.REJECTED);
            default -> List.of(TaskStatus.values());
        };
    }

    private void recalculateEventProgress(Event event) {
        if (event.getStatus() == EventStatus.CERRADO) {
            return;
        }

        long total = eventRepository.countTotalTasksByEventId(event.getId());
        if (total == 0) {
            event.setStatus(EventStatus.PLANIFICACION);
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);
            return;
        }

        long completed = eventRepository.countTasksWithCompletedStatus(event.getId());
        long rejected = eventRepository.countTasksWithRejectedStatus(event.getId());
        long finalized = completed + rejected;
        long confirmed = eventRepository.countTasksWithConfirmedStatus(event.getId());
        long inPreparation = eventRepository.countTasksWithInPreparationStatus(event.getId());
        long accepted = eventRepository.countTasksWithAcceptedStatus(event.getId());

        EventStatus newStatus;
        if (finalized == total) {
            newStatus = EventStatus.CIERRE;
        } else if (confirmed > 0) {
            newStatus = EventStatus.CONFIRMACION;
        } else if (inPreparation > 0 || accepted > 0) {
            newStatus = EventStatus.PREPARACION;
        } else {
            newStatus = EventStatus.PLANIFICACION;
        }

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private void validateEventNotClosed(Event event) {
        if (event.getStatus() == EventStatus.CERRADO) {
            throw new IllegalStateException("El acto esta cerrado y no permite modificaciones");
        }
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

    private Task findTaskOrThrow(Integer taskId, Integer eventId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId + " en el acto: " + eventId));
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

    private TaskResponse toResponse(Task task) {
        Integer assignedToId = null;
        String assignedToName = null;
        if (task.getAssignedTo() != null) {
            assignedToId = task.getAssignedTo().getId();
            assignedToName = task.getAssignedTo().getFullName();
        }

        Integer createdByUserId = task.getCreatedBy() != null ? task.getCreatedBy().getId() : null;

        Integer confirmedById = null;
        String confirmedByName = null;
        if (task.getConfirmedBy() != null) {
            confirmedById = task.getConfirmedBy().getId();
            confirmedByName = task.getConfirmedBy().getFullName();
        }

        return new TaskResponse(
                task.getId(),
                task.getEvent().getId(),
                task.getTitle(),
                task.getDescription(),
                assignedToId,
                assignedToName,
                createdByUserId,
                task.getStatus().name(),
                task.getDeadline(),
                task.getRejectionReason(),
                confirmedById,
                confirmedByName,
                task.getConfirmedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private MyTaskResponse toMyTaskResponse(Task task) {
        return new MyTaskResponse(
                task.getId(),
                task.getEvent().getId(),
                task.getEvent().getEventType().name(),
                task.getEvent().getTitle(),
                task.getTitle(),
                task.getStatus().name(),
                task.getDeadline(),
                task.getRejectionReason(),
                task.getConfirmedAt(),
                task.getCompletedAt(),
                task.getUpdatedAt()
        );
    }
}

