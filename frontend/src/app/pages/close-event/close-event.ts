import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EventService } from '../../services/event.service';
import { TaskService } from '../../services/task.service';
import { IncidentService } from '../../services/incident.service';
import { EventResponse } from '../../models/event.model';
import { TaskResponse } from '../../models/task.model';
import { IncidentResponse } from '../../models/incident.model';

interface BlockingItem {
  type: 'TAREA' | 'INCIDENCIA';
  label: string;
  status: string;
}

@Component({
  selector: 'app-close-event',
  templateUrl: './close-event.html',
  styleUrl: './close-event.scss',
})
export class CloseEvent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly taskService = inject(TaskService);
  private readonly incidentService = inject(IncidentService);

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
    const eventId = Number(this.route.snapshot.paramMap.get('eventId'));
    this.loadEventData(eventId);
  }

  close(): void {
    if (this.event && !this.isBlocked && !this.closing) {
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
    this.router.navigate(['/']);
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.goBack();
    }
  }

  private loadEventData(eventId: number): void {
    forkJoin({
      event: this.eventService.findById(eventId),
      tasks: this.taskService.findByEventId(eventId),
      incidents: this.incidentService.findByEventId(eventId)
    }).subscribe({
      next: (data) => {
        this.event = data.event;
        this.blockingItems = this.buildBlockingItems(data.tasks, data.incidents);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo cargar la información del acto.';
        this.loading = false;
      }
    });
  }

  private buildBlockingItems(tasks: TaskResponse[], incidents: IncidentResponse[]): BlockingItem[] {
    const items: BlockingItem[] = [];

    for (const task of tasks) {
      if (task.status !== 'CONFIRMADA') {
        items.push({
          type: 'TAREA',
          label: task.title,
          status: task.status === 'PENDIENTE' ? 'PENDIENTE' : 'RECHAZADA'
        });
      }
    }

    for (const incident of incidents) {
      if (incident.status === 'ABIERTA') {
        items.push({
          type: 'INCIDENCIA',
          label: incident.description,
          status: 'ABIERTA'
        });
      }
    }

    return items;
  }
}
