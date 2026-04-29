package com.actacofrade.backend.util;

import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationHelperTest {

    private AuthorizationHelper helper;
    private Hermandad hermandad;
    private User admin;
    private User responsable;
    private User colaborador;
    private User consultor;
    private Event event;

    @BeforeEach
    void setUp() {
        helper = new AuthorizationHelper();
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@x.com", hermandad, RoleCode.ADMINISTRADOR);
        responsable = TestFixtures.user(2, "resp@x.com", hermandad, RoleCode.RESPONSABLE);
        colaborador = TestFixtures.user(3, "col@x.com", hermandad, RoleCode.COLABORADOR);
        consultor = TestFixtures.user(4, "cons@x.com", hermandad, RoleCode.CONSULTA);
        event = TestFixtures.event(10, hermandad, responsable);
    }

    @Test
    @DisplayName("isOwner returns true only when ids match and not null")
    void isOwner() {
        assertThat(helper.isOwner(1, 1)).isTrue();
        assertThat(helper.isOwner(1, 2)).isFalse();
        assertThat(helper.isOwner(1, null)).isFalse();
    }

    @Test
    void isResponsible_trueWhenMatch() {
        assertThat(helper.isResponsible(responsable.getId(), event)).isTrue();
        assertThat(helper.isResponsible(colaborador.getId(), event)).isFalse();
    }

    @Test
    void isResponsible_falseWhenNoResponsible() {
        Event e = TestFixtures.event(2, hermandad, null);
        assertThat(helper.isResponsible(1, e)).isFalse();
    }

    @Test
    void isAdmin_detectsAdminRole() {
        assertThat(helper.isAdmin(admin)).isTrue();
        assertThat(helper.isAdmin(responsable)).isFalse();
    }

    @Test
    void isConsultor_detectsConsultaRole() {
        assertThat(helper.isConsultor(consultor)).isTrue();
        assertThat(helper.isConsultor(colaborador)).isFalse();
    }

    @Test
    void canManageAct_adminAlways() {
        assertThat(helper.canManageAct(admin, event)).isTrue();
    }

    @Test
    void canManageAct_responsibleOfEvent() {
        assertThat(helper.canManageAct(responsable, event)).isTrue();
    }

    @Test
    void canManageAct_collaboratorCannot() {
        assertThat(helper.canManageAct(colaborador, event)).isFalse();
    }

    @Test
    void actsAsCollaboratorInEvent_isInverse() {
        assertThat(helper.actsAsCollaboratorInEvent(colaborador, event)).isTrue();
        assertThat(helper.actsAsCollaboratorInEvent(admin, event)).isFalse();
    }

    @Test
    void requireAssignable_rejectsConsulta() {
        assertThatThrownBy(() -> helper.requireAssignable(consultor))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAssignable_acceptsNullAndOthers() {
        helper.requireAssignable(null);
        helper.requireAssignable(colaborador);
    }

    @Test
    void requireEventManager_throwsForCollaborator() {
        assertThatThrownBy(() -> helper.requireEventManager(event, colaborador))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireEventManager_passesForAdmin() {
        helper.requireEventManager(event, admin);
    }

    @Test
    void requireAdmin_throwsForNonAdmin() {
        assertThatThrownBy(() -> helper.requireAdmin(colaborador))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAdmin_passesForAdmin() {
        helper.requireAdmin(admin);
    }

    @Test
    void isTaskClosed_recognizesClosedStatuses() {
        Task t1 = TestFixtures.task(1, event, colaborador, TaskStatus.COMPLETED);
        Task t2 = TestFixtures.task(2, event, colaborador, TaskStatus.REJECTED);
        Task t3 = TestFixtures.task(3, event, colaborador, TaskStatus.PLANNED);
        assertThat(helper.isTaskClosed(t1)).isTrue();
        assertThat(helper.isTaskClosed(t2)).isTrue();
        assertThat(helper.isTaskClosed(t3)).isFalse();
        assertThat(helper.isTaskClosed(null)).isFalse();
    }

    @Test
    void requireTaskNotClosedOrAdmin_blocksNonAdminClosed() {
        Task t = TestFixtures.task(1, event, colaborador, TaskStatus.COMPLETED);
        assertThatThrownBy(() -> helper.requireTaskNotClosedOrAdmin(t, colaborador))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireTaskNotClosedOrAdmin_allowsAdminOnClosed() {
        Task t = TestFixtures.task(1, event, colaborador, TaskStatus.COMPLETED);
        helper.requireTaskNotClosedOrAdmin(t, admin);
    }

    @Test
    void requireTaskNotClosedOrAdmin_allowsAnyoneOnOpen() {
        Task t = TestFixtures.task(1, event, colaborador, TaskStatus.PLANNED);
        helper.requireTaskNotClosedOrAdmin(t, colaborador);
    }

    @Test
    void requireTaskAssigned_throwsWhenNotAssigned() {
        Task t = TestFixtures.task(1, event, colaborador, TaskStatus.PLANNED);
        assertThatThrownBy(() -> helper.requireTaskAssigned(t, responsable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireTaskAssigned_passesWhenAssigned() {
        Task t = TestFixtures.task(1, event, colaborador, TaskStatus.PLANNED);
        helper.requireTaskAssigned(t, colaborador);
    }

    @Test
    void hasRole_works() {
        assertThat(helper.hasRole(responsable, RoleCode.RESPONSABLE)).isTrue();
        assertThat(helper.hasRole(responsable, RoleCode.ADMINISTRADOR)).isFalse();
    }

    @Test
    void isTaskAssignedToUser_handlesNullAssignee() {
        Task t = TestFixtures.task(1, event, null, TaskStatus.PLANNED);
        assertThat(helper.isTaskAssignedToUser(1, t)).isFalse();
    }
}
