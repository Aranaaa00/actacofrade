package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
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

    public List<TaskResponse> findByEventId(Integer eventId) {
        findEventOrThrow(eventId);
        return taskRepository.findByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse findById(Integer eventId, Integer taskId) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);
        return toResponse(task);
    }

    public TaskResponse create(Integer eventId, CreateTaskRequest request) {
        Event event = findEventOrThrow(eventId);
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

    public TaskResponse update(Integer eventId, Integer taskId, UpdateTaskRequest request) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);

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

    public void delete(Integer eventId, Integer taskId) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);
        taskRepository.delete(task);
    }

    public TaskResponse confirm(Integer eventId, Integer taskId) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.CONFIRMADA) {
            throw new IllegalStateException("La tarea ya esta confirmada");
        }

        task.setStatus(TaskStatus.CONFIRMADA);
        task.setRejectionReason(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse reject(Integer eventId, Integer taskId, String rejectionReason) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.RECHAZADA) {
            throw new IllegalStateException("La tarea ya esta rechazada");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalStateException("El motivo de rechazo es obligatorio");
        }

        task.setStatus(TaskStatus.RECHAZADA);
        task.setRejectionReason(rejectionReason);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse resetToPending(Integer eventId, Integer taskId) {
        findEventOrThrow(eventId);
        Task task = findTaskOrThrow(taskId, eventId);

        if (task.getStatus() == TaskStatus.PENDIENTE) {
            throw new IllegalStateException("La tarea ya esta pendiente");
        }

        task.setStatus(TaskStatus.PENDIENTE);
        task.setRejectionReason(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return toResponse(task);
    }

    private Event findEventOrThrow(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Acto no encontrado con id: " + eventId));
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

    private TaskResponse toResponse(Task task) {
        Integer assignedToId = null;
        String assignedToName = null;
        if (task.getAssignedTo() != null) {
            assignedToId = task.getAssignedTo().getId();
            assignedToName = task.getAssignedTo().getFullName();
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
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
