package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.MyTaskStatsResponse;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import com.actacofrade.backend.util.AuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private final AuthorizationHelper authorizationHelper = new AuthorizationHelper();

    private TaskService service;

    private Hermandad hermandad;
    private User admin;
    private User responsable;
    private User colaborador;
    private User consultor;
    private Event event;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, eventRepository, userRepository, auditLogService, authorizationHelper);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        responsable = TestFixtures.user(2, "resp@e.com", hermandad, RoleCode.RESPONSABLE);
        colaborador = TestFixtures.user(3, "col@e.com", hermandad, RoleCode.COLABORADOR);
        consultor = TestFixtures.user(4, "cons@e.com", hermandad, RoleCode.CONSULTA);
        event = TestFixtures.event(10, hermandad, responsable);
        task = TestFixtures.task(20, event, colaborador, TaskStatus.PLANNED);
    }

    private void mockUser(User u) {
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    private void mockEvent() {
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
    }

    private void mockTask() {
        when(taskRepository.findById(20)).thenReturn(Optional.of(task));
    }

    @Test
    void findByEventId_returnsTasks() {
        mockUser(admin);
        mockEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of(task));
        List<TaskResponse> res = service.findByEventId(10, admin.getEmail());
        assertThat(res).hasSize(1);
    }

    @Test
    void findById_returnsTask() {
        mockUser(admin);
        mockEvent();
        mockTask();
        TaskResponse res = service.findById(10, 20, admin.getEmail());
        assertThat(res.id()).isEqualTo(20);
    }

    @Test
    void create_admin_assignsToRequestedUser() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        CreateTaskRequest req = new CreateTaskRequest("T", "d", 3, LocalDate.now().plusDays(1));

        TaskResponse res = service.create(10, req, admin.getEmail());
        assertThat(res.assignedToId()).isEqualTo(3);
    }

    @Test
    void create_collaborator_isForcedAsAssignee() {
        mockUser(colaborador);
        mockEvent();
        CreateTaskRequest req = new CreateTaskRequest("T", null, 999, null);
        TaskResponse res = service.create(10, req, colaborador.getEmail());
        assertThat(res.assignedToId()).isEqualTo(colaborador.getId());
    }

    @Test
    void create_assignToConsultor_throws() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(4)).thenReturn(Optional.of(consultor));
        CreateTaskRequest req = new CreateTaskRequest("T", null, 4, null);
        assertThatThrownBy(() -> service.create(10, req, admin.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_eventClosed_throws() {
        event.setStatus(EventStatus.CERRADO);
        mockUser(admin);
        mockEvent();
        CreateTaskRequest req = new CreateTaskRequest("T", null, null, null);
        assertThatThrownBy(() -> service.create(10, req, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_collaboratorOnSomeoneElsesTask_denied() {
        Task other = TestFixtures.task(21, event, responsable, TaskStatus.PLANNED);
        other.setCreatedBy(responsable);
        mockUser(colaborador);
        mockEvent();
        when(taskRepository.findById(21)).thenReturn(Optional.of(other));

        UpdateTaskRequest req = new UpdateTaskRequest("nuevo", null, null, null);
        assertThatThrownBy(() -> service.update(10, 21, req, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_collaboratorReassigningToOther_denied() {
        task.setCreatedBy(colaborador);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        UpdateTaskRequest req = new UpdateTaskRequest(null, null, 999, null);
        assertThatThrownBy(() -> service.update(10, 20, req, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_admin_changesAllFields() {
        mockUser(admin);
        mockEvent();
        mockTask();
        when(userRepository.findById(2)).thenReturn(Optional.of(responsable));
        UpdateTaskRequest req = new UpdateTaskRequest("Nuevo", "Desc", 2, LocalDate.now().plusDays(2));
        TaskResponse res = service.update(10, 20, req, admin.getEmail());
        assertThat(res.title()).isEqualTo("Nuevo");
        assertThat(res.assignedToId()).isEqualTo(2);
    }

    @Test
    void update_closedTask_collaboratorDenied() {
        task.setStatus(TaskStatus.COMPLETED);
        task.setCreatedBy(colaborador);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.update(10, 20, new UpdateTaskRequest("x", null, null, null), colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_admin_deletes() {
        mockUser(admin);
        mockEvent();
        mockTask();
        service.delete(10, 20, admin.getEmail());
        verify(taskRepository).delete(task);
    }

    @Test
    void delete_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.delete(10, 20, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void accept_validTransition() {
        mockUser(admin);
        mockEvent();
        mockTask();
        TaskResponse res = service.accept(10, 20, admin.getEmail());
        assertThat(res.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void accept_wrongStatus_throws() {
        task.setStatus(TaskStatus.CONFIRMED);
        mockUser(admin);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.accept(10, 20, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startPreparation_requiresAssignee() {
        task.setStatus(TaskStatus.ACCEPTED);
        mockUser(responsable);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.startPreparation(10, 20, responsable.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void startPreparation_assignee_succeeds() {
        task.setStatus(TaskStatus.ACCEPTED);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        TaskResponse res = service.startPreparation(10, 20, colaborador.getEmail());
        assertThat(res.status()).isEqualTo("IN_PREPARATION");
    }

    @Test
    void startPreparation_wrongStatus_throws() {
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.startPreparation(10, 20, colaborador.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirm_assignee_succeeds() {
        task.setStatus(TaskStatus.IN_PREPARATION);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        TaskResponse res = service.confirm(10, 20, colaborador.getEmail());
        assertThat(res.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirm_wrongStatus_throws() {
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.confirm(10, 20, colaborador.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void complete_assignee_succeeds() {
        task.setStatus(TaskStatus.CONFIRMED);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        TaskResponse res = service.complete(10, 20, colaborador.getEmail());
        assertThat(res.status()).isEqualTo("COMPLETED");
    }

    @Test
    void complete_wrongStatus_throws() {
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.complete(10, 20, colaborador.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reject_admin_succeeds() {
        mockUser(admin);
        mockEvent();
        mockTask();
        TaskResponse res = service.reject(10, 20, "motivo", admin.getEmail());
        assertThat(res.status()).isEqualTo("REJECTED");
    }

    @Test
    void reject_emptyReason_throws() {
        mockUser(admin);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.reject(10, 20, "  ", admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reject_alreadyRejected_throws() {
        task.setStatus(TaskStatus.REJECTED);
        mockUser(admin);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.reject(10, 20, "x", admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reject_completed_throws() {
        task.setStatus(TaskStatus.COMPLETED);
        mockUser(admin);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.reject(10, 20, "x", admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resetToPlanned_admin_resets() {
        task.setStatus(TaskStatus.COMPLETED);
        mockUser(admin);
        mockEvent();
        mockTask();
        TaskResponse res = service.resetToPlanned(10, 20, admin.getEmail());
        assertThat(res.status()).isEqualTo("PLANNED");
    }

    @Test
    void resetToPlanned_alreadyPlanned_throws() {
        mockUser(admin);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.resetToPlanned(10, 20, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resetToPlanned_collaboratorOnClosed_denied() {
        task.setStatus(TaskStatus.COMPLETED);
        mockUser(colaborador);
        mockEvent();
        mockTask();
        assertThatThrownBy(() -> service.resetToPlanned(10, 20, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMyTaskStats_aggregatesCounts() {
        mockUser(colaborador);
        when(taskRepository.countByAssignedToIdAndStatusAndEventHermandadId(eq(3), eq(TaskStatus.PLANNED), eq(1))).thenReturn(2L);
        when(taskRepository.countByAssignedToIdAndStatusInAndEventHermandadId(eq(3), any(), eq(1))).thenReturn(5L);
        when(taskRepository.countByAssignedToIdAndStatusAndEventHermandadId(eq(3), eq(TaskStatus.REJECTED), eq(1))).thenReturn(1L);

        MyTaskStatsResponse stats = service.getMyTaskStats(colaborador.getEmail());
        assertThat(stats.pendingCount()).isEqualTo(2);
        assertThat(stats.confirmedCount()).isEqualTo(5);
        assertThat(stats.rejectedCount()).isEqualTo(1);
    }

    @Test
    void getMyTaskStats_userWithoutHermandad_throws() {
        User u = TestFixtures.user(50, "n@e.com", null, RoleCode.COLABORADOR);
        mockUser(u);
        assertThatThrownBy(() -> service.getMyTaskStats(u.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_recalculatesEventProgress_updatesStatus() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        when(eventRepository.countTotalTasksByEventId(10)).thenReturn(2L);
        when(eventRepository.countTasksWithCompletedStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithRejectedStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithConfirmedStatus(10)).thenReturn(1L);
        when(eventRepository.countTasksWithInPreparationStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithAcceptedStatus(10)).thenReturn(0L);

        CreateTaskRequest req = new CreateTaskRequest("T", null, 3, null);
        service.create(10, req, admin.getEmail());

        assertThat(event.getStatus()).isEqualTo(EventStatus.CONFIRMACION);
    }

    @Test
    void create_zeroTasks_setsPlanificacion() {
        event.setStatus(EventStatus.PREPARACION);
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        when(eventRepository.countTotalTasksByEventId(10)).thenReturn(0L);

        service.create(10, new CreateTaskRequest("T", null, 3, null), admin.getEmail());
        assertThat(event.getStatus()).isEqualTo(EventStatus.PLANIFICACION);
    }

    @Test
    void recalculate_finalized_setsCierre() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        when(eventRepository.countTotalTasksByEventId(10)).thenReturn(2L);
        when(eventRepository.countTasksWithCompletedStatus(10)).thenReturn(1L);
        when(eventRepository.countTasksWithRejectedStatus(10)).thenReturn(1L);

        service.create(10, new CreateTaskRequest("T", null, 3, null), admin.getEmail());
        assertThat(event.getStatus()).isEqualTo(EventStatus.CIERRE);
    }

    @Test
    void recalculate_acceptedOrInPrep_setsPreparacion() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        when(eventRepository.countTotalTasksByEventId(10)).thenReturn(2L);
        when(eventRepository.countTasksWithCompletedStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithRejectedStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithConfirmedStatus(10)).thenReturn(0L);
        when(eventRepository.countTasksWithInPreparationStatus(10)).thenReturn(1L);
        when(eventRepository.countTasksWithAcceptedStatus(10)).thenReturn(0L);

        service.create(10, new CreateTaskRequest("T", null, 3, null), admin.getEmail());
        assertThat(event.getStatus()).isEqualTo(EventStatus.PREPARACION);
    }

    @Test
    void recalculate_skippedWhenClosed() {
        event.setStatus(EventStatus.CERRADO);
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.create(10, new CreateTaskRequest("T", null, null, null), admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
        verify(eventRepository, never()).countTotalTasksByEventId(any());
    }
}
