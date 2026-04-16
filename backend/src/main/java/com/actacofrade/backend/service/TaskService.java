package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TaskService(TaskRepository taskRepository, EventRepository eventRepository,
                       UserRepository userRepository, AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
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
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);

        User assignedTo = resolveUser(request.assignedToId());
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);

        Task task = new Task();
        task.setEvent(event);
        task.setTitle(SanitizationUtils.sanitize(request.title()));
        task.setDescription(SanitizationUtils.sanitizeNullable(request.description()));
        task.setAssignedTo(assignedTo);
        task.setDeadline(request.deadline());

        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_CREATED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse update(Integer eventId, Integer taskId, UpdateTaskRequest request, String authenticatedEmail) {
        Event event = findEventOrThrow(eventId);
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        boolean isColaboradorOnly = currentUser.getRoles().stream()
                .allMatch(r -> r.getCode() == RoleCode.COLABORADOR);
        if (isColaboradorOnly) {
            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Solo puedes editar las tareas que tienes asignadas");
            }
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
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
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
        validateOwnershipOrManager(task, currentUser);

        task.setStatus(TaskStatus.IN_PREPARATION);
        task.setRejectionReason(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_ACCEPTED", currentUser, task.getTitle());
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
        validateOwnershipOrManager(task, confirmer);

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
        validateOwnershipOrManager(task, currentUser);

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
        boolean isManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getCode() == RoleCode.ADMINISTRADOR || r.getCode() == RoleCode.RESPONSABLE);
        if (!isManager) {
            boolean isAssigned = task.getAssignedTo() != null
                    && task.getAssignedTo().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new AccessDeniedException("Solo puedes rechazar tus propias tareas");
            }
        }

        task.setStatus(TaskStatus.REJECTED);
        task.setRejectionReason(SanitizationUtils.sanitize(rejectionReason));
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        auditLogService.log(event, "TASK", task.getId(), "TASK_REJECTED", currentUser, task.getTitle());
        recalculateEventProgress(event);
        return toResponse(task);
    }

    public TaskResponse resetToPlanned(Integer eventId, Integer taskId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.PLANNED) {
            throw new IllegalStateException("La tarea ya esta en estado PLANNED");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);

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
        long confirmed = eventRepository.countTasksWithConfirmedStatus(event.getId());
        long inPreparation = eventRepository.countTasksWithInPreparationStatus(event.getId());

        EventStatus newStatus;
        if (completed == total) {
            newStatus = EventStatus.CIERRE;
        } else if (confirmed > 0) {
            newStatus = EventStatus.CONFIRMACION;
        } else if (inPreparation > 0) {
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

    private Event findEventOrThrow(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Acto no encontrado con id: " + eventId));
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

    private void validateOwnershipOrManager(Task task, User user) {
        boolean isAssigned = task.getAssignedTo() != null
                && task.getAssignedTo().getId().equals(user.getId());
        boolean isManager = user.getRoles().stream()
                .anyMatch(r -> r.getCode() == RoleCode.ADMINISTRADOR || r.getCode() == RoleCode.RESPONSABLE);
        if (!isAssigned && !isManager) {
            throw new AccessDeniedException("Solo el usuario asignado o un responsable puede actuar sobre esta tarea");
        }
    }

    private TaskResponse toResponse(Task task) {
        Integer assignedToId = null;
        String assignedToName = null;
        if (task.getAssignedTo() != null) {
            assignedToId = task.getAssignedTo().getId();
            assignedToName = task.getAssignedTo().getFullName();
        }

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
}

