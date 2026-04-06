import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

interface MockEvent {
  id: number;
  title: string;
  status: string;
  statusLabel: string;
  eventType: string;
}

interface MockAlert {
  type: 'TAREA' | 'INCIDENCIA' | 'CIERRE';
  description: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly authService = inject(AuthService);

  readonly events: MockEvent[] = [
    {
      id: 1,
      title: 'Cabildo General Ordinario de Cuentas',
      status: 'in-progress',
      statusLabel: 'En proceso',
      eventType: 'CABILDO'
    },
    {
      id: 2,
      title: 'Solemne Quinario al Santísimo Cristo',
      status: 'blocked',
      statusLabel: 'Bloqueado',
      eventType: 'CULTOS'
    },
    {
      id: 3,
      title: 'Estación de Penitencia — Viernes Santo',
      status: 'planning',
      statusLabel: 'Planificación',
      eventType: 'PROCESIÓN'
    }
  ];

  readonly alerts: MockAlert[] = [
    {
      type: 'TAREA',
      description: 'Validar presupuesto anual adjunto al acta de cuentas.'
    },
    {
      type: 'INCIDENCIA',
      description: 'Falta firma digital del Hermano Mayor en certificación.'
    },
    {
      type: 'CIERRE',
      description: 'Expediente listo para archivo definitivo y foliación.'
    }
  ];

  readonly pendingDecisionsCount = 8;
  readonly readyToCloseCount = 3;
  readonly pendingTasksCount = 3;

  get userName(): string {
    const user = this.authService.getUser();
    let name = 'usuario';
    if (user?.fullName) {
      name = user.fullName.split(' ')[0];
    }
    return name;
  }

  getStatusBadgeClass(status: string): string {
    const classMap: Record<string, string> = {
      'in-progress': 'badge--confirmed',
      'blocked': 'badge--rejected',
      'planning': 'badge--neutral',
    };
    return classMap[status] || 'badge--neutral';
  }
}
