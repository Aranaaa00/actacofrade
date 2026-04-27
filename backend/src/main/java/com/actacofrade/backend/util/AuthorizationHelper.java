package com.actacofrade.backend.util;

import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationHelper {

    public boolean isOwner(Integer userId, Integer resourceOwnerId) {
        return resourceOwnerId != null && resourceOwnerId.equals(userId);
    }

    public boolean isResponsible(Integer userId, Event event) {
        return event.getResponsible() != null && event.getResponsible().getId().equals(userId);
    }

    public boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.ADMINISTRADOR);
    }

    public boolean isColaboradorOnly(User user) {
        return user.getRoles().stream().allMatch(r -> r.getCode() == RoleCode.COLABORADOR);
    }

    public boolean canManageAct(User user, Event event) {
        return isAdmin(user) || isResponsible(user.getId(), event);
    }

    public void requireEventManager(Event event, User currentUser) {
        if (!canManageAct(currentUser, event)) {
            throw new AccessDeniedException("Solo puedes gestionar los actos de los que eres responsable");
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
