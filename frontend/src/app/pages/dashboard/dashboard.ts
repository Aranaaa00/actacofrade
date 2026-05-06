import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { ToastService } from '../../services/toast.service';
import { EventResponse } from '../../models/event.model';
import { DashboardAlert, DashboardAlertType } from '../../models/dashboard.model';
import { Badge } from '../../shared/components/badge/badge';
import { getEventStatusLabel, getEventStatusBadgeVariant } from '../../shared/utils/label-maps.utils';

const ALERT_TYPE_LABELS: Record<DashboardAlertType, string> = {
  TASK: 'TAREA',
  INCIDENT: 'INCIDENCIA',
  DECISION: 'DECISIÓN'
};

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Badge, LucideAngularModule],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly toast = inject(ToastService);

  events: EventResponse[] = [];
  alerts: DashboardAlert[] = [];
  alertCount = 0;
  readyToCloseCount = 0;
  loading = true;

  get userName(): string {
    const user = this.authService.getUser();
    let name = 'usuario';
    if (user?.fullName) {
      name = user.fullName.split(' ')[0];
    }
    return name;
  }

  get isConsulta(): boolean {
    return this.authService.isConsulta();
  }

  ngOnInit(): void {
    // single endpoint that aggregates events, alerts and counters in one network call
    this.dashboardService.getDashboard().subscribe({
      next: data => {
        this.events = data.recentEvents;
        this.alerts = data.alerts;
        this.alertCount = data.pendingTasksCount;
        this.readyToCloseCount = data.readyToCloseCount;
        this.loading = false;
      },
      error: (err) => {
        this.toast.fromHttpErrorSilencingAuth(err, 'No se pudieron cargar los datos. Inténtalo de nuevo más tarde.');
        this.loading = false;
      }
    });
  }

  getStatusLabel = getEventStatusLabel;
  getStatusBadgeVariant = getEventStatusBadgeVariant;

  getAlertTypeLabel(type: DashboardAlertType): string {
    return ALERT_TYPE_LABELS[type];
  }
}
