package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
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

    public TaskService(TaskRepository taskRepository, EventRepository eventRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
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
        User assignedTo = resolveUser(request.assignedToId());

        Task task = new Task();
        task.setEvent(event);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssignedTo(assignedTo);
        task.setDeadline(request.deadline());

        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse update(Integer eventId, Integer taskId, UpdateTaskRequest request, String authenticatedEmail) {
        findEventOrThrow(eventId);
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
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.assignedToId() != null) {
            task.setAssignedTo(resolveUser(request.assignedToId()));
        }
        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }

        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    public void delete(Integer eventId, Integer taskId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Task task = findTaskOrThrow(taskId, eventId);
        taskRepository.delete(task);
    }

    public TaskResponse confirm(Integer eventId, Integer taskId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.CONFIRMADA) {
            throw new IllegalStateException("La tarea ya esta confirmada");
        }

        User confirmer = findUserByEmailOrThrow(authenticatedEmail);

        task.setStatus(TaskStatus.CONFIRMADA);
        task.setRejectionReason(null);
        task.setConfirmedBy(confirmer);
        task.setConfirmedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse reject(Integer eventId, Integer taskId, String rejectionReason, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.RECHAZADA) {
            throw new IllegalStateException("La tarea ya esta rechazada");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalStateException("El motivo de rechazo es obligatorio");
        }

        User confirmer = findUserByEmailOrThrow(authenticatedEmail);

        task.setStatus(TaskStatus.RECHAZADA);
        task.setRejectionReason(rejectionReason);
        task.setConfirmedBy(confirmer);
        task.setConfirmedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse resetToPending(Integer eventId, Integer taskId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.PENDIENTE) {
            throw new IllegalStateException("La tarea ya esta pendiente");
        }

        task.setStatus(TaskStatus.PENDIENTE);
        task.setRejectionReason(null);
        task.setConfirmedBy(null);
        task.setConfirmedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
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
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
