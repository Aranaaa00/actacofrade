import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Banner } from '../../shared/components/banner/banner';
import { Pagination } from '../../shared/components/pagination/pagination';
import { Tabs } from '../../shared/components/tabs/tabs';
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
  imports: [Badge, Banner, Pagination, Tabs, LucideAngularModule, ElementForm, CloseEvent, FormsModule],
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
  rejectionReason = '';
  rejectSubmitted = false;
  processingTaskId: number | null = null;

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
    'ACCEPTED': 0.25,
    'IN_PREPARATION': 0.5,
    'CONFIRMED': 0.75,
    'COMPLETED': 1,
    'REJECTED': 0
  };

  private readonly decisionWeights: Record<string, number> = {
    'PENDIENTE': 0,
    'LISTA': 1,
    'RECHAZADA': 0
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

    return Math.round((weightedSum / totalElements) * 100);
  }

  get steps(): StepInfo[] {
    const progress = this.progressPercent;
    const thresholds = [0, 25, 50, 75];
    return this.stepKeys.map((key, i) => ({
      key,
      label: getStepLabel(key),
      done: progress >= thresholds[i],
      connectorDone: i < this.stepKeys.length - 1 && progress >= thresholds[i + 1]
    }));
  }

  get statusVariant(): string {
    const status = this.event?.status || '';
    return status === 'CERRADO' ? 'confirmed' : 'pending';
  }

  get unconfirmedTasksCount(): number {
    return this.tasks.filter(t => t.status !== 'COMPLETED').length;
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
    return task.assignedToId === userId || this.auth.canManage();
  }

  canActOnIncident(incident: IncidentResponse): boolean {
    if (incident.status === 'RESUELTA') {
      return this.auth.isAdmin();
    }
    return this.auth.canManage();
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
    this.rejectionReason = '';
    this.rejectSubmitted = false;
    this.showRejectModal = true;
  }

  cancelReject(): void {
    this.showRejectModal = false;
    this.rejectingTaskId = null;
    this.rejectionReason = '';
    this.rejectSubmitted = false;
  }

  submitReject(): void {
    this.rejectSubmitted = true;
    const sanitizedReason = sanitizeText(this.rejectionReason);
    if (!this.rejectingTaskId || !sanitizedReason) {
      return;
    }
    this.processingTaskId = this.rejectingTaskId;
    this.taskService.reject(this.eventId, this.rejectingTaskId, sanitizedReason).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.showRejectModal = false;
        this.rejectingTaskId = null;
        this.rejectionReason = '';
        this.rejectSubmitted = false;
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
