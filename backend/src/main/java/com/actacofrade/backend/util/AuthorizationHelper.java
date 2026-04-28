package com.actacofrade.backend.util;

import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthorizationHelper {

    private static final Set<TaskStatus> CLOSED_TASK_STATUSES = Set.of(TaskStatus.COMPLETED, TaskStatus.REJECTED);

    public boolean isOwner(Integer userId, Integer resourceOwnerId) {
        return resourceOwnerId != null && resourceOwnerId.equals(userId);
    }

    public boolean isResponsible(Integer userId, Event event) {
        return event.getResponsible() != null && event.getResponsible().getId().equals(userId);
    }

    public boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.ADMINISTRADOR);
    }

    public boolean hasRole(User user, RoleCode code) {
        return user.getRoles().stream().anyMatch(r -> r.getCode() == code);
    }

    public boolean isConsultor(User user) {
        return hasRole(user, RoleCode.CONSULTA);
    }

    public boolean canManageAct(User user, Event event) {
        return isAdmin(user) || isResponsible(user.getId(), event);
    }

    public boolean actsAsCollaboratorInEvent(User user, Event event) {
        return !canManageAct(user, event);
    }

    public boolean isTaskClosed(Task task) {
        return task != null && CLOSED_TASK_STATUSES.contains(task.getStatus());
    }

    public void requireAssignable(User user) {
        if (user != null && isConsultor(user)) {
            throw new AccessDeniedException("No se puede asignar a un usuario con rol CONSULTA");
        }
    }

    public void requireEventManager(Event event, User currentUser) {
        if (!canManageAct(currentUser, event)) {
            throw new AccessDeniedException("Solo puedes gestionar los actos de los que eres responsable");
        }
    }

    public void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Solo un administrador puede realizar esta accion");
        }
    }

    public void requireTaskNotClosedOrAdmin(Task task, User currentUser) {
        if (isTaskClosed(task) && !isAdmin(currentUser)) {
            throw new AccessDeniedException("La tarea esta cerrada y solo un administrador puede modificarla");
        }
    }

    public boolean isTaskAssignedToUser(Integer userId, Task task) {
        return task.getAssignedTo() != null && task.getAssignedTo().getId().equals(userId);
    }

    public void requireTaskAssigned(Task task, User currentUser) {
        if (!isTaskAssignedToUser(currentUser.getId(), task)) {
            throw new AccessDeniedException("Solo el usuario asignado puede avanzar el estado de esta tarea");
        }
    }
}
