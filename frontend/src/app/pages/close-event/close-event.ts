import { Component, OnInit, inject, Input, Output, EventEmitter } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { EventService } from '../../services/event.service';
import { TaskService } from '../../services/task.service';
import { DecisionService } from '../../services/decision.service';
import { IncidentService } from '../../services/incident.service';
import { EventResponse } from '../../models/event.model';
import { TaskResponse } from '../../models/task.model';
import { DecisionResponse } from '../../models/decision.model';
import { IncidentResponse } from '../../models/incident.model';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { Banner } from '../../shared/components/banner/banner';
import { Badge } from '../../shared/components/badge/badge';

interface BlockingItem {
  type: 'TAREA' | 'INCIDENCIA' | 'DECISIÓN';
  label: string;
  status: string;
}

@Component({
  selector: 'app-close-event',
  imports: [ModalOverlay, Banner, Badge, LucideAngularModule],
  templateUrl: './close-event.html',
})
export class CloseEvent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly taskService = inject(TaskService);
  private readonly decisionService = inject(DecisionService);
  private readonly incidentService = inject(IncidentService);

  @Input() embedded = false;
  @Input() inputEventId?: number;
  @Output() closed = new EventEmitter<void>();

  event: EventResponse | null = null;
  blockingItems: BlockingItem[] = [];
  loading = true;
  closing = false;
  errorMessage = '';
  successMessage = '';

  get isBlocked(): boolean {
    return this.blockingItems.length > 0;
  }

  ngOnInit(): void {
    const eventId = this.embedded && this.inputEventId
      ? this.inputEventId
      : Number(this.route.snapshot.paramMap.get('eventId'));
    this.loadEventData(eventId);
  }

  close(): void {
    if (this.event && !this.closing) {
      this.closing = true;
      this.errorMessage = '';

      this.eventService.close(this.event.id).subscribe({
        next: () => {
          this.successMessage = 'El acto ha sido cerrado correctamente.';
          this.closing = false;
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Ha ocurrido un error al cerrar el acto.';
          this.closing = false;
        }
      });
    }
  }

  goBack(): void {
    if (this.embedded) {
      this.closed.emit();
    } else {
      this.router.navigate(['/']);
    }
  }

  private loadEventData(eventId: number): void {
    forkJoin({
      event: this.eventService.findById(eventId),
      tasks: this.taskService.findByEventId(eventId),
      decisions: this.decisionService.findByEventId(eventId),
      incidents: this.incidentService.findByEventId(eventId)
    }).subscribe({
      next: (data) => {
        this.event = data.event;
        this.blockingItems = this.buildBlockingItems(data.tasks, data.decisions, data.incidents);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo cargar la información del acto.';
        this.loading = false;
      }
    });
  }

  private readonly taskStatusLabelMap: Record<string, string> = {
    'PLANNED': 'Planificada',
    'ACCEPTED': 'Aceptada',
    'IN_PREPARATION': 'En preparación',
    'CONFIRMED': 'Confirmada',
    'COMPLETED': 'Completada',
    'REJECTED': 'Rechazada',
  };

  private buildBlockingItems(tasks: TaskResponse[], decisions: DecisionResponse[], incidents: IncidentResponse[]): BlockingItem[] {
    const items: BlockingItem[] = [];

    for (const task of tasks) {
      if (task.status !== 'COMPLETED' && task.status !== 'REJECTED') {
        items.push({
          type: 'TAREA',
          label: task.title,
          status: this.taskStatusLabelMap[task.status] || task.status
        });
      }
    }

    for (const decision of decisions) {
      if (decision.status === 'PENDING') {
        items.push({
          type: 'DECISIÓN',
          label: decision.title,
          status: 'Pendiente'
        });
      }
    }

    for (const incident of incidents) {
      if (incident.status === 'ABIERTA') {
        items.push({
          type: 'INCIDENCIA',
          label: incident.description,
          status: 'Abierta'
        });
      }
    }

    return items;
  }
}
