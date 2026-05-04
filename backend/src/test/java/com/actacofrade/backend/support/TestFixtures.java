package com.actacofrade.backend.support;

import com.actacofrade.backend.entity.Decision;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.EventType;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.HermandadArea;
import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Hermandad hermandad(Integer id, String nombre) {
        Hermandad h = new Hermandad();
        h.setId(id);
        h.setNombre(nombre);
        return h;
    }

    public static Role role(Integer id, RoleCode code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    public static User user(Integer id, String email, Hermandad hermandad, RoleCode... codes) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName("User " + id);
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        u.setActive(true);
        u.setHermandad(hermandad);
        Set<Role> roles = new HashSet<>();
        for (int i = 0; i < codes.length; i++) {
            roles.add(role(i + 1, codes[i]));
        }
        u.setRoles(roles);
        return u;
    }

    public static Event event(Integer id, Hermandad hermandad, User responsible) {
        Event e = new Event();
        e.setId(id);
        e.setReference("2026/0001");
        e.setTitle("Acto " + id);
        e.setEventType(EventType.CABILDO);
        e.setEventDate(LocalDate.now().plusDays(30));
        e.setStatus(EventStatus.PLANNING);
        e.setHermandad(hermandad);
        e.setResponsible(responsible);
        e.setIsLockedForClosing(false);
        return e;
    }

    public static Task task(Integer id, Event event, User assignedTo, TaskStatus status) {
        Task t = new Task();
        t.setId(id);
        t.setTitle("Tarea " + id);
        t.setEvent(event);
        t.setAssignedTo(assignedTo);
        t.setStatus(status);
        t.setCreatedBy(assignedTo);
        return t;
    }

    public static Decision decision(Integer id, Event event, User reviewedBy, DecisionStatus status) {
        Decision d = new Decision();
        d.setId(id);
        d.setTitle("Decision " + id);
        d.setArea(HermandadArea.SECRETARIA);
        d.setEvent(event);
        d.setReviewedBy(reviewedBy);
        d.setStatus(status);
        return d;
    }

    public static Incident incident(Integer id, Event event, User reportedBy, IncidentStatus status) {
        Incident i = new Incident();
        i.setId(id);
        i.setDescription("Incidencia " + id);
        i.setEvent(event);
        i.setReportedBy(reportedBy);
        i.setStatus(status);
        return i;
    }
}
