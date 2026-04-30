import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Banner } from '../../shared/components/banner/banner';
import { Pagination } from '../../shared/components/pagination/pagination';
import { Tabs } from '../../shared/components/tabs/tabs';
import { RejectModal } from '../../shared/components/reject-modal/reject-modal';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { ElementForm } from '../element-form/element-form';
import { CloseEvent } from '../close-event/close-event';
import { EventService } from '../../services/event.service';
import { TaskService } from '../../services/task.service';
import { DecisionService } from '../../services/decision.service';
import { IncidentService } from '../../services/incident.service';
import { AuditLogService } from '../../services/audit-log.service';
import { AuthService } from '../../services/auth.service';
import { EventResponse } from '../../models/event.model';
import { TaskResponse } from '../../models/task.model';
import { DecisionResponse } from '../../models/decision.model';
import { IncidentResponse } from '../../models/incident.model';
import { AuditLogResponse } from '../../models/audit-log.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';
import {
  getEventTypeLabel, getEventStatusLabel,
  getTaskStatusLabel, getTaskBadgeVariant,
  getDecisionStatusLabel, getDecisionBadgeVariant,
  getIncidentStatusLabel, getIncidentBadgeVariant,
  getAreaLabel, getActionLabel, getEntityTypeLabel,
  getHistoryBadgeVariant
} from '../../shared/utils/label-maps.utils';
import { formatDate, formatDateTime } from '../../shared/utils/date.utils';
import {
  calculateActProgress, getProgressMessage, getPendingActionsText,
  buildProgressSteps, ActProgressStep
} from '../../shared/utils/act-progress.utils';

type ElementTab = 'task' | 'decision' | 'incident';

interface EditData {
  type: ElementTab;
  id: number;
  title: string;
  assignedToId: number | null;
  deadline: string;
  notes: string;
  area: string;
}

@Component({
  selector: 'app-act-detail',
  imports: [Badge, Banner, Pagination, Tabs, LucideAngularModule, ElementForm, CloseEvent, RejectModal, ModalOverlay, ConfirmDialog],
  templateUrl: './act-detail.html',
})
export class ActDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly taskService = inject(TaskService);
  private readonly decisionService = inject(DecisionService);
  private readonly incidentService = inject(IncidentService);
  private readonly auditLogService = inject(AuditLogService);
  readonly auth = inject(AuthService);

  eventId = 0;
  selectedTab = 'Tareas';
  readonly tabLabels = ['Tareas', 'Decisiones', 'Incidencias', 'Historial'];

  showElementForm = false;
  elementFormTab: ElementTab = 'task';
  elementEditData: EditData | null = null;
  showCloseEvent = false;
  loading = true;

  showRejectModal = false;
  rejectingTaskId: number | null = null;
  processingTaskId: number | null = null;

  showExportModal = false;
  exportFormat: 'PDF' | 'CSV' = 'PDF';
  exportSections: string[] = ['TASKS', 'DECISIONS', 'INCIDENTS'];
  exportSubmitted = false;
  exportLoading = false;
  exportError = '';

  // global error message for header actions like clone
  actionError = '';

  readonly exportSectionOptions = [
    { value: 'TASKS', label: 'Tareas y responsables' },
    { value: 'DECISIONS', label: 'Decisiones tomadas' },
    { value: 'INCIDENTS', label: 'Incidencias registradas' },
    { value: 'OBSERVATIONS', label: 'Observaciones del acto' },
  ];

  readonly exportFormatOptions: ReadonlyArray<{ value: 'PDF' | 'CSV'; label: string; description: string }> = [
    { value: 'PDF', label: 'PDF', description: 'Documento con membrete' },
    { value: 'CSV', label: 'CSV', description: 'Tabla para Excel' },
  ];

  event: EventResponse | null = null;
  tasks: TaskResponse[] = [];
  decisions: DecisionResponse[] = [];
  incidents: IncidentResponse[] = [];

  historyEntries: AuditLogResponse[] = [];
  historyPage = 1;
  historyTotalPages = 1;
  historyLoading = false;
  private readonly historyPageSize = 5;

  get statusLabel(): string {
    return getEventStatusLabel(this.event?.status || '');
  }

  get progress() {
    if (this.event?.status === 'CLOSED') {
      const total = this.tasks.length + this.decisions.length + this.incidents.length;
      return { total, completed: total, pending: 0, percent: 100 };
    }
    return calculateActProgress(this.tasks, this.decisions, this.incidents);
  }

  get progressPercent(): number {
    return this.progress.percent;
  }

  get pendingActionsCount(): number {
    return this.progress.pending;
  }

  get pendingActionsText(): string {
    return getPendingActionsText(this.pendingActionsCount);
  }

  get progressMessage(): string {
    const { percent, total } = this.progress;
    return getProgressMessage(percent, total);
  }

  get steps(): ActProgressStep[] {
    return buildProgressSteps(this.progressPercent);
  }

  get statusVariant(): string {
    const status = this.event?.status || '';
    return status === 'CLOSED' ? 'confirmed' : 'pending';
  }

  get hasPendingItems(): boolean {
    return this.pendingActionsCount > 0;
  }


  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  getTaskBadgeVariant = getTaskBadgeVariant;
  getTaskStatusLabel = getTaskStatusLabel;
  getDecisionBadgeVariant = getDecisionBadgeVariant;
  getDecisionStatusLabel = getDecisionStatusLabel;
  getIncidentBadgeVariant = getIncidentBadgeVariant;
  getIncidentStatusLabel = getIncidentStatusLabel;
  getEventTypeLabel = getEventTypeLabel;
  getAreaLabel = getAreaLabel;

  cloneAct(): void {
    // reset previous error before request
    this.actionError = '';
    this.eventService.clone(this.eventId).subscribe({
      next: (cloned) => {
        this.router.navigate(['/events', cloned.id]);
      },
      error: () => {
        // notify user when cloning fails
        this.actionError = 'No se pudo clonar el acto. Inténtalo de nuevo.';
      }
    });
  }

  dismissActionError(): void {
    this.actionError = '';
  }

  openExportModal(): void {
    this.exportFormat = 'PDF';
    this.exportSections = ['TASKS', 'DECISIONS', 'INCIDENTS'];
    this.exportSubmitted = false;
    this.exportError = '';
    this.showExportModal = true;
  }

  closeExportModal(): void {
    this.showExportModal = false;
  }

  selectExportFormat(format: 'PDF' | 'CSV'): void {
    this.exportFormat = format;
  }

  isExportSectionSelected(value: string): boolean {
    return this.exportSections.includes(value);
  }

  toggleExportSection(value: string): void {
    const idx = this.exportSections.indexOf(value);
    if (idx === -1) {
      this.exportSections = [...this.exportSections, value];
    } else {
      this.exportSections = this.exportSections.filter(s => s !== value);
    }
  }

  doExport(): void {
    this.exportSubmitted = true;
    if (this.exportSections.length === 0) {
      return;
    }
    this.exportLoading = true;
    this.exportError = '';
    this.eventService.export(this.eventId, this.exportFormat, this.exportSections).subscribe({
      next: (blob: Blob) => {
        // trigger browser download for the generated file
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `acto-${this.eventId}.${this.exportFormat.toLowerCase()}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exportLoading = false;
        this.closeExportModal();
      },
      error: () => {
        // show error inside modal so the user can retry
        this.exportLoading = false;
        this.exportError = 'No se pudo generar el archivo. Inténtalo de nuevo.';
      }
    });
  }

  addTask(): void {
    this.elementFormTab = 'task';
    this.elementEditData = null;
    this.showElementForm = true;
  }

  addDecision(): void {
    this.elementFormTab = 'decision';
    this.elementEditData = null;
    this.showElementForm = true;
  }

  addIncident(): void {
    this.elementFormTab = 'incident';
    this.elementEditData = null;
    this.showElementForm = true;
  }

  onElementSaved(): void {
    this.showElementForm = false;
    this.elementEditData = null;
    this.loadData();
  }

  onElementClosed(): void {
    this.showElementForm = false;
    this.elementEditData = null;
  }

  openCloseEvent(): void {
    this.showCloseEvent = true;
  }

  onCloseEventClosed(): void {
    this.showCloseEvent = false;
    this.loadData();
  }

  canActOnTask(task: TaskResponse): boolean {
    const userId = this.auth.getUserId();
    return task.assignedToId === userId;
  }

  canManageTask(_task: TaskResponse): boolean {
    return this.auth.canManageAct(this.event);
  }

  canEditTask(task: TaskResponse): boolean {
    const userId = this.auth.getUserId();
    if (this.auth.canManageAct(this.event)) return true;
    return task.createdByUserId === userId;
  }

  canActOnIncident(_incident: IncidentResponse): boolean {
    return this.auth.canManageAct(this.event);
  }

  acceptTask(task: TaskResponse): void {
    this.runTaskAction(task.id, this.taskService.accept(this.eventId, task.id));
  }

  startPreparation(task: TaskResponse): void {
    this.runTaskAction(task.id, this.taskService.startPreparation(this.eventId, task.id));
  }

  confirmTask(task: TaskResponse): void {
    this.runTaskAction(task.id, this.taskService.confirm(this.eventId, task.id));
  }

  completeTask(task: TaskResponse): void {
    this.runTaskAction(task.id, this.taskService.complete(this.eventId, task.id));
  }

  openRejectModal(task: TaskResponse): void {
    this.rejectingTaskId = task.id;
    this.showRejectModal = true;
  }

  onRejectCancelled(): void {
    this.showRejectModal = false;
    this.rejectingTaskId = null;
  }

  onRejectConfirmed(reason: string): void {
    if (!this.rejectingTaskId) {
      return;
    }
    const taskId = this.rejectingTaskId;
    this.runTaskAction(
      taskId,
      this.taskService.reject(this.eventId, taskId, sanitizeText(reason)),
      () => {
        this.showRejectModal = false;
        this.rejectingTaskId = null;
      }
    );
  }

  resetTask(task: TaskResponse): void {
    this.runTaskAction(task.id, this.taskService.reset(this.eventId, task.id));
  }

  private runTaskAction(taskId: number, request$: Observable<unknown>, onSuccess?: () => void): void {
    this.processingTaskId = taskId;
    request$.subscribe({
      next: () => {
        this.processingTaskId = null;
        onSuccess?.();
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  editTask(task: TaskResponse): void {
    this.elementFormTab = 'task';
    this.elementEditData = {
      type: 'task',
      id: task.id,
      title: task.title,
      assignedToId: task.assignedToId,
      deadline: task.deadline || '',
      notes: task.description,
      area: ''
    };
    this.showElementForm = true;
  }

  editDecision(decision: DecisionResponse): void {
    this.elementFormTab = 'decision';
    this.elementEditData = {
      type: 'decision',
      id: decision.id,
      title: decision.title,
      assignedToId: decision.reviewedById,
      deadline: '',
      notes: '',
      area: decision.area
    };
    this.showElementForm = true;
  }

  deleteTask(taskId: number): void {
    this.pendingDelete = { type: 'task', id: taskId };
  }

  deleteDecision(decisionId: number): void {
    this.pendingDelete = { type: 'decision', id: decisionId };
  }

  deleteIncident(incidentId: number): void {
    this.pendingDelete = { type: 'incident', id: incidentId };
  }

  pendingDelete: { type: ElementTab; id: number } | null = null;

  get pendingDeleteMessage(): string {
    if (!this.pendingDelete) return '';
    const labels: Record<ElementTab, string> = {
      task: 'esta tarea',
      decision: 'esta decisión',
      incident: 'esta incidencia',
    };
    return `¿Seguro que deseas eliminar ${labels[this.pendingDelete.type]}? Esta acción no se puede deshacer.`;
  }

  cancelPendingDelete(): void {
    this.pendingDelete = null;
  }

  confirmPendingDelete(): void {
    if (!this.pendingDelete) return;
    const { type, id } = this.pendingDelete;
    this.pendingDelete = null;
    let request$: Observable<void>;
    if (type === 'task') request$ = this.taskService.delete(this.eventId, id);
    else if (type === 'decision') request$ = this.decisionService.delete(this.eventId, id);
    else request$ = this.incidentService.delete(this.eventId, id);
    request$.subscribe({
      next: () => this.loadData(),
      error: () => {
        // surface deletion failure to the user
        this.actionError = 'No se pudo eliminar el elemento. Inténtalo de nuevo.';
      }
    });
  }

  acceptDecision(decision: DecisionResponse): void {
    this.decisionService.accept(this.eventId, decision.id).subscribe({
      next: () => this.loadData(),
      error: () => {
        this.actionError = 'No se pudo aceptar la decisión. Inténtalo de nuevo.';
      }
    });
  }

  rejectDecision(decision: DecisionResponse): void {
    this.decisionService.reject(this.eventId, decision.id).subscribe({
      next: () => this.loadData(),
      error: () => {
        this.actionError = 'No se pudo rechazar la decisión. Inténtalo de nuevo.';
      }
    });
  }

  resolveIncident(incidentId: number): void {
    this.incidentService.resolve(this.eventId, incidentId).subscribe({
      next: () => this.loadData(),
      error: () => {
        this.actionError = 'No se pudo resolver la incidencia. Inténtalo de nuevo.';
      }
    });
  }

  formatDate = formatDate;
  formatDateTime = formatDateTime;
  getActionLabel = getActionLabel;
  getEntityTypeLabel = getEntityTypeLabel;
  getHistoryBadgeVariant = getHistoryBadgeVariant;

  onTabChange(tab: string): void {
    this.selectedTab = tab;
    if (tab === 'Historial' && this.historyEntries.length === 0) {
      this.loadHistory(1);
    }
  }

  loadHistory(page: number): void {
    this.historyPage = page;
    this.historyLoading = true;
    this.auditLogService.findByEventId(this.eventId, page - 1, this.historyPageSize).subscribe({
      next: (data) => {
        this.historyEntries = data.content;
        this.historyTotalPages = Math.max(1, data.totalPages);
        this.historyLoading = false;
      },
      error: () => {
        this.historyLoading = false;
      }
    });
  }

  get isEventClosed(): boolean {
    return this.event?.status === 'CLOSED';
  }

  private loadData(): void {
    forkJoin({
      event: this.eventService.findById(this.eventId),
      tasks: this.taskService.findByEventId(this.eventId),
      decisions: this.decisionService.findByEventId(this.eventId),
      incidents: this.incidentService.findByEventId(this.eventId)
    }).subscribe({
      next: (data) => {
        this.event = data.event;
        this.tasks = data.tasks;
        this.decisions = data.decisions;
        this.incidents = data.incidents;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
