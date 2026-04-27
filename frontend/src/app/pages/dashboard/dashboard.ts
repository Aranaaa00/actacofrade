import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../services/auth.service';
import { EventService } from '../../services/event.service';
import { TaskService } from '../../services/task.service';
import { EventResponse } from '../../models/event.model';
import { MyTaskStats } from '../../models/task.model';
import { Badge } from '../../shared/components/badge/badge';
import { getEventStatusLabel, getEventStatusBadgeVariant } from '../../shared/utils/label-maps.utils';

interface Alert {
  type: 'TAREA' | 'INCIDENCIA' | 'CIERRE';
  description: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Badge, LucideAngularModule],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly eventService = inject(EventService);
  private readonly taskService = inject(TaskService);

  events: EventResponse[] = [];
  alerts: Alert[] = [];
  myTaskStats: MyTaskStats = { pendingCount: 0, confirmedCount: 0, rejectedCount: 0 };
  loading = true;
  errorMessage = '';

  get pendingTasksCount(): number {
    return this.myTaskStats.pendingCount;
  }

  get readyToCloseCount(): number {
    return this.events.filter(e =>
      e.status === 'CONFIRMACION' && e.pendingTasks === 0 && e.openIncidents === 0
    ).length;
  }

  get userName(): string {
    const user = this.authService.getUser();
    let name = 'usuario';
    if (user?.fullName) {
      name = user.fullName.split(' ')[0];
    }
    return name;
  }

  ngOnInit(): void {
    forkJoin({
      events: this.eventService.findAll(),
      stats: this.taskService.getMyTaskStats()
    }).subscribe({
      next: ({ events, stats }) => {
        this.events = events;
        this.myTaskStats = stats;
        this.buildAlerts();
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los datos. Inténtalo de nuevo más tarde.';
        this.loading = false;
      }
    });
  }

  getStatusLabel = getEventStatusLabel;
  getStatusBadgeVariant = getEventStatusBadgeVariant;

  private buildAlerts(): void {
    const result: Alert[] = [];
    for (const event of this.events) {
      if (event.pendingTasks > 0) {
        result.push({
          type: 'TAREA',
          description: `${event.pendingTasks} tarea(s) pendiente(s) en «${event.title}».`
        });
      }
      if (event.openIncidents > 0) {
        result.push({
          type: 'INCIDENCIA',
          description: `${event.openIncidents} incidencia(s) abierta(s) en «${event.title}».`
        });
      }
      if (event.status === 'CONFIRMACION' && event.pendingTasks === 0 && event.openIncidents === 0) {
        result.push({
          type: 'CIERRE',
          description: `«${event.title}» listo para cerrar.`
        });
      }
    }
    this.alerts = result;
  }
}
