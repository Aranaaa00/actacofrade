import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../services/auth.service';
import { EventService } from '../../services/event.service';
import { EventResponse } from '../../models/event.model';
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

  events: EventResponse[] = [];
  alerts: Alert[] = [];
  loading = true;

  get pendingTasksCount(): number {
    return this.events.reduce((sum, e) => sum + e.pendingTasks, 0);
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
    this.eventService.findAll().subscribe({
      next: (events) => {
        this.events = events;
        this.buildAlerts();
        this.loading = false;
      },
      error: () => {
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
