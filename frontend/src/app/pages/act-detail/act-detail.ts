import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Banner } from '../../shared/components/banner/banner';
import { Tabs } from '../../shared/components/tabs/tabs';

interface MockEvent {
  id: number;
  reference: string;
  title: string;
  eventType: string;
  eventDate: string;
  location: string;
  responsibleName: string;
  status: string;
  statusLabel: string;
}

interface MockStep {
  key: string;
  label: string;
  done: boolean;
}

interface MockTask {
  id: number;
  title: string;
  description: string;
  assignedToName: string;
  status: string;
  deadline: string;
}

interface MockDecision {
  id: number;
  area: string;
  title: string;
  status: string;
  statusLabel: string;
  reviewedByName: string;
}

interface MockIncident {
  id: number;
  description: string;
  status: string;
  statusLabel: string;
  reportedByName: string;
  resolvedByName: string;
}

@Component({
  selector: 'app-act-detail',
  imports: [RouterLink, Badge, Banner, Tabs, LucideAngularModule],
  templateUrl: './act-detail.html',
})
export class ActDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);

  eventId = 0;
  selectedTab = 'Tareas';
  readonly tabLabels = ['Tareas', 'Decisiones', 'Incidencias', 'Historial del acto'];

  readonly event: MockEvent = {
    id: 1,
    reference: '2026/0042',
    title: 'Cabildo General Ordinario',
    eventType: 'CABILDO',
    eventDate: '15 Mar 2026',
    location: 'Casa Hermandad',
    responsibleName: 'Mayordomía',
    status: 'EN_PROCESO',
    statusLabel: 'En proceso'
  };

  readonly steps: MockStep[] = [
    { key: 'planificacion', label: 'Planificación', done: true },
    { key: 'preparacion', label: 'Preparación', done: true },
    { key: 'confirmacion', label: 'Confirmación', done: false },
    { key: 'cierre', label: 'Cierre', done: false }
  ];

  readonly tasks: MockTask[] = [
    {
      id: 1,
      title: 'Preparación de Censo Electoral',
      description: 'Revisión de altas y bajas del último trimestre',
      assignedToName: 'M. López',
      status: 'PENDIENTE',
      deadline: '12 Mar 2026'
    },
    {
      id: 2,
      title: 'Reserva de Casa Hermandad',
      description: '',
      assignedToName: 'J. Riva',
      status: 'CONFIRMADA',
      deadline: '01 Mar 2026'
    },
    {
      id: 3,
      title: 'Contratación de Catering',
      description: 'Motivo: No se realizará...',
      assignedToName: 'F. García',
      status: 'RECHAZADA',
      deadline: '05 Mar 2026'
    }
  ];

  readonly decisions: MockDecision[] = [
    {
      id: 1,
      area: 'Economía',
      title: 'Aprobación de presupuesto anual 2026',
      status: 'APROBADA',
      statusLabel: 'Aprobada',
      reviewedByName: 'M. Arana'
    },
    {
      id: 2,
      area: 'Cultos',
      title: 'Cambio de horario del quinario',
      status: 'PENDIENTE',
      statusLabel: 'Pendiente',
      reviewedByName: 'J. López'
    }
  ];

  readonly incidents: MockIncident[] = [
    {
      id: 1,
      description: 'Avería en sistema de megafonía de la Casa Hermandad',
      status: 'ABIERTA',
      statusLabel: 'Abierta',
      reportedByName: 'F. García',
      resolvedByName: ''
    }
  ];

  get statusVariant(): string {
    const variantMap: Record<string, string> = {
      'EN_PROCESO': 'pending',
      'CERRADO': 'neutral',
      'COMPLETADO': 'confirmed',
    };
    return variantMap[this.event.status] || 'neutral';
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
      'APROBADA': 'confirmed',
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
}
