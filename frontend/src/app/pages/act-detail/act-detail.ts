import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Banner } from '../../shared/components/banner/banner';
import { Pagination } from '../../shared/components/pagination/pagination';
import { Tabs } from '../../shared/components/tabs/tabs';
import { RejectModal } from '../../shared/components/reject-modal/reject-modal';
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
  getHistoryBadgeVariant, getStepLabel
} from '../../shared/utils/label-maps.utils';
import { formatDate, formatDateTime } from '../../shared/utils/date.utils';

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

interface StepInfo {
  key: string;
  label: string;
  done: boolean;
  connectorDone: boolean;
}

@Component({
  selector: 'app-act-detail',
  imports: [Badge, Banner, Pagination, Tabs, LucideAngularModule, ElementForm, CloseEvent, FormsModule, RejectModal],
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
  readonly tabLabels = ['Tareas', 'Decisiones', 'Incidencias', 'Historial del acto'];

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

  readonly exportSectionOptions = [
    { value: 'OBSERVATIONS', label: 'Observaciones' },
    { value: 'TASKS', label: 'Tareas' },
    { value: 'DECISIONS', label: 'Decisiones' },
    { value: 'INCIDENTS', label: 'Incidencias' },
    { value: 'HISTORY', label: 'Historial' },
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

  private readonly stepKeys = ['PLANIFICACION', 'PREPARACION', 'CONFIRMACION', 'CIERRE'];

  private readonly taskWeights: Record<string, number> = {
    'PLANNED': 0,
    'ACCEPTED': 0.2,
    'IN_PREPARATION': 0.4,
    'CONFIRMED': 0.6,
    'COMPLETED': 1,
    'REJECTED': 1
  };

  private readonly decisionWeights: Record<string, number> = {
    'PENDING': 0,
    'ACCEPTED': 1,
    'REJECTED': 1
  };

  private readonly incidentWeights: Record<string, number> = {
    'ABIERTA': 0,
    'RESUELTA': 1
  };

  get statusLabel(): string {
    return getEventStatusLabel(this.event?.status || '');
  }

  get progressPercent(): number {
    if (this.event?.status === 'CERRADO') {
      return 100;
    }

    let totalElements = 0;
    let weightedSum = 0;

    for (const task of this.tasks) {
      totalElements++;
      weightedSum += this.taskWeights[task.status] ?? 0;
    }

    for (const decision of this.decisions) {
      totalElements++;
      weightedSum += this.decisionWeights[decision.status] ?? 0;
    }

    for (const incident of this.incidents) {
      totalElements++;
      weightedSum += this.incidentWeights[incident.status] ?? 0;
    }

    if (totalElements === 0) {
      return 0;
    }

    return (weightedSum / totalElements) * 100;
  }

  get steps(): StepInfo[] {
    const progress = this.progressPercent;
    const stepCount = this.stepKeys.length;
    return this.stepKeys.map((key, i) => {
      const stepThreshold = (i / (stepCount - 1)) * 100;
      const nextThreshold = ((i + 1) / (stepCount - 1)) * 100;
      return {
        key,
        label: getStepLabel(key),
        done: progress >= stepThreshold,
        connectorDone: i < stepCount - 1 && progress >= nextThreshold
      };
    });
  }

  get statusVariant(): string {
    const status = this.event?.status || '';
    return status === 'CERRADO' ? 'confirmed' : 'pending';
  }

  get unconfirmedTasksCount(): number {
    return this.tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'REJECTED').length;
  }

  get openIncidentsCount(): number {
    return this.incidents.filter(i => i.status === 'ABIERTA').length;
  }

  get hasPendingItems(): boolean {
    return this.unconfirmedTasksCount > 0 || this.openIncidentsCount > 0;
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
    this.eventService.clone(this.eventId).subscribe({
      next: (cloned) => {
        this.router.navigate(['/events', cloned.id]);
      }
    });
  }

  openExportModal(): void {
    this.exportSubmitted = false;
    this.showExportModal = true;
  }

  closeExportModal(): void {
    this.showExportModal = false;
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
    this.eventService.export(this.eventId, this.exportFormat, this.exportSections).subscribe({
      next: (blob: Blob) => {
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
        this.exportLoading = false;
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
    this.processingTaskId = task.id;
    this.taskService.accept(this.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  startPreparation(task: TaskResponse): void {
    this.processingTaskId = task.id;
    this.taskService.startPreparation(this.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  confirmTask(task: TaskResponse): void {
    this.processingTaskId = task.id;
    this.taskService.confirm(this.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  completeTask(task: TaskResponse): void {
    this.processingTaskId = task.id;
    this.taskService.complete(this.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
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
    this.processingTaskId = this.rejectingTaskId;
    this.taskService.reject(this.eventId, this.rejectingTaskId, sanitizeText(reason)).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.showRejectModal = false;
        this.rejectingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  resetTask(task: TaskResponse): void {
    this.processingTaskId = task.id;
    this.taskService.reset(this.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
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
    this.taskService.delete(this.eventId, taskId).subscribe({
      next: () => this.loadData()
    });
  }

  deleteDecision(decisionId: number): void {
    this.decisionService.delete(this.eventId, decisionId).subscribe({
      next: () => this.loadData()
    });
  }

  acceptDecision(decision: DecisionResponse): void {
    this.decisionService.accept(this.eventId, decision.id).subscribe({
      next: () => this.loadData()
    });
  }

  rejectDecision(decision: DecisionResponse): void {
    this.decisionService.reject(this.eventId, decision.id).subscribe({
      next: () => this.loadData()
    });
  }

  deleteIncident(incidentId: number): void {
    this.incidentService.delete(this.eventId, incidentId).subscribe({
      next: () => this.loadData()
    });
  }

  resolveIncident(incidentId: number): void {
    this.incidentService.resolve(this.eventId, incidentId).subscribe({
      next: () => this.loadData()
    });
  }

  formatDate = formatDate;
  formatDateTime = formatDateTime;
  getActionLabel = getActionLabel;
  getEntityTypeLabel = getEntityTypeLabel;
  getHistoryBadgeVariant = getHistoryBadgeVariant;

  onTabChange(tab: string): void {
    this.selectedTab = tab;
    if (tab === 'Historial del acto' && this.historyEntries.length === 0) {
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
    return this.event?.status === 'CERRADO';
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
