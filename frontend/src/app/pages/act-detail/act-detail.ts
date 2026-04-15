import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
import { AuthService } from '../../services/auth.service';
import { EventResponse } from '../../models/event.model';
import { TaskResponse } from '../../models/task.model';
import { DecisionResponse } from '../../models/decision.model';
import { IncidentResponse } from '../../models/incident.model';

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
  imports: [Badge, Banner, Tabs, LucideAngularModule, ElementForm, CloseEvent],
  templateUrl: './act-detail.html',
})
export class ActDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly taskService = inject(TaskService);
  private readonly decisionService = inject(DecisionService);
  private readonly incidentService = inject(IncidentService);
  readonly auth = inject(AuthService);

  eventId = 0;
  selectedTab = 'Tareas';
  readonly tabLabels = ['Tareas', 'Decisiones', 'Incidencias', 'Historial del acto'];

  showElementForm = false;
  elementFormTab: ElementTab = 'task';
  elementEditData: EditData | null = null;
  showCloseEvent = false;
  loading = true;

  event: EventResponse | null = null;
  tasks: TaskResponse[] = [];
  decisions: DecisionResponse[] = [];
  incidents: IncidentResponse[] = [];

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
    return this.tasks.filter(t => t.status !== 'CONFIRMADA').length;
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
      'PENDIENTE': 'pending',
      'CONFIRMADA': 'confirmed',
      'RECHAZADA': 'rejected',
    };
    return variantMap[status] || 'neutral';
  }

  getDecisionBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'LISTA': 'confirmed',
      'PENDIENTE': 'pending',
      'RECHAZADA': 'rejected',
    };
    return variantMap[status] || 'neutral';
  }

  getIncidentBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'ABIERTA': 'pending',
      'RESUELTA': 'confirmed',
    };
    return variantMap[status] || 'neutral';
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

  formatDate(dateStr: string | null): string {
    if (!dateStr) {
      return '—';
    }
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
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
