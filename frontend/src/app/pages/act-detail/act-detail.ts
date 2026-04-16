import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Banner } from '../../shared/components/banner/banner';
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
  imports: [Badge, Banner, Tabs, LucideAngularModule, ElementForm, CloseEvent, FormsModule],
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
  historyPage = 0;
  historyTotalPages = 0;
  historyLoading = false;
  private readonly historyPageSize = 5;

  private readonly stepKeys = ['PLANIFICACION', 'PREPARACION', 'CONFIRMACION', 'CIERRE'];
  private readonly stepLabels: Record<string, string> = {
    PLANIFICACION: 'Planificación',
    PREPARACION: 'Preparación',
    CONFIRMACION: 'Confirmación',
    CIERRE: 'Cierre'
  };

  private readonly statusLabelMap: Record<string, string> = {
    PLANIFICACION: 'En planificación',
    PREPARACION: 'En proceso',
    CONFIRMACION: 'En confirmación',
    CIERRE: 'En cierre',
    CERRADO: 'Cerrado'
  };

  get statusLabel(): string {
    return this.statusLabelMap[this.event?.status || ''] || this.event?.status || '';
  }

  get steps(): StepInfo[] {
    const currentIndex = this.stepKeys.indexOf(this.event?.status || '');
    return this.stepKeys.map((key, i) => ({
      key,
      label: this.stepLabels[key],
      done: i <= currentIndex,
      connectorDone: i < currentIndex
    }));
  }

  get statusVariant(): string {
    const variantMap: Record<string, string> = {
      'PLANIFICACION': 'pending',
      'PREPARACION': 'pending',
      'CONFIRMACION': 'pending',
      'CIERRE': 'pending',
      'CERRADO': 'confirmed',
    };
    return variantMap[this.event?.status || ''] || 'pending';
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

  getTaskBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'PLANNED': 'pending',
      'ACCEPTED': 'pending',
      'IN_PREPARATION': 'pending',
      'CONFIRMED': 'confirmed',
      'COMPLETED': 'confirmed',
      'REJECTED': 'rejected',
    };
    return variantMap[status] || 'neutral';
  }

  getTaskStatusLabel(status: string): string {
    const labelMap: Record<string, string> = {
      'PLANNED': 'Planificada',
      'ACCEPTED': 'Aceptada',
      'IN_PREPARATION': 'En preparación',
      'CONFIRMED': 'Confirmada',
      'COMPLETED': 'Completada',
      'REJECTED': 'Rechazada',
    };
    return labelMap[status] || status;
  }

  getDecisionBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'LISTA': 'confirmed',
      'PENDIENTE': 'pending',
      'RECHAZADA': 'rejected',
    };
    return variantMap[status] || 'neutral';
  }

  getDecisionStatusLabel(status: string): string {
    const labelMap: Record<string, string> = {
      'LISTA': 'Lista',
      'PENDIENTE': 'Pendiente',
      'RECHAZADA': 'Rechazada',
    };
    return labelMap[status] || status;
  }

  getIncidentBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'ABIERTA': 'pending',
      'RESUELTA': 'confirmed',
    };
    return variantMap[status] || 'neutral';
  }

  getIncidentStatusLabel(status: string): string {
    const labelMap: Record<string, string> = {
      'ABIERTA': 'Abierta',
      'RESUELTA': 'Resuelta',
    };
    return labelMap[status] || status;
  }

  getEventTypeLabel(type: string): string {
    const typeMap: Record<string, string> = {
      'CABILDO': 'Cabildo',
      'CULTOS': 'Cultos',
      'PROCESION': 'Procesión',
      'ENSAYO': 'Ensayo',
      'OTRO': 'Otro',
    };
    return typeMap[type] || type;
  }

  getAreaLabel(area: string): string {
    const areaMap: Record<string, string> = {
      'MAYORDOMIA': 'Mayordomía',
      'SECRETARIA': 'Secretaría',
      'PRIOSTIA': 'Priostía',
      'TESORERIA': 'Tesorería',
      'DIPUTACION_MAYOR': 'Diputación Mayor',
    };
    return areaMap[area] || area;
  }

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

  formatDate(dateStr: string | null): string {
    if (!dateStr) {
      return '—';
    }
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(dateStr: string | null): string {
    if (!dateStr) {
      return '—';
    }
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  getActionLabel(action: string): string {
    const actionMap: Record<string, string> = {
      'TASK_CREATED': 'Tarea creada',
      'TASK_ACCEPTED': 'Tarea aceptada',
      'TASK_IN_PREPARATION': 'Tarea en preparación',
      'TASK_CONFIRMED': 'Tarea confirmada',
      'TASK_COMPLETED': 'Tarea completada',
      'TASK_REJECTED': 'Tarea rechazada',
      'TASK_RESET': 'Tarea reiniciada',
      'TASK_UPDATED': 'Tarea actualizada',
      'TASK_DELETED': 'Tarea eliminada',
      'DECISION_CREATED': 'Decisión creada',
      'DECISION_READY': 'Decisión lista',
      'DECISION_REJECTED': 'Decisión rechazada',
      'INCIDENT_CREATED': 'Incidencia creada',
      'INCIDENT_RESOLVED': 'Incidencia resuelta',
      'EVENT_CLOSED': 'Acto cerrado',
    };
    return actionMap[action] || action;
  }

  getEntityTypeLabel(entityType: string): string {
    const typeMap: Record<string, string> = {
      'TASK': 'Tarea',
      'DECISION': 'Decisión',
      'INCIDENT': 'Incidencia',
      'EVENT': 'Acto',
    };
    return typeMap[entityType] || entityType;
  }

  onTabChange(tab: string): void {
    this.selectedTab = tab;
    if (tab === 'Historial del acto' && this.historyEntries.length === 0) {
      this.loadHistory(0);
    }
  }

  loadHistory(page: number): void {
    this.historyLoading = true;
    this.auditLogService.findByEventId(this.eventId, page, this.historyPageSize).subscribe({
      next: (data) => {
        this.historyEntries = data.content;
        this.historyPage = data.number;
        this.historyTotalPages = data.totalPages;
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
