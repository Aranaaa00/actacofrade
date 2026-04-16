import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { ActEditor } from '../act-editor/act-editor';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { EventResponse } from '../../models/event.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';

@Component({
  selector: 'app-act-list',
  imports: [RouterLink, Badge, LucideAngularModule, Datepicker, ActEditor],
  templateUrl: './act-list.html',
})
export class ActList implements OnInit {
  private readonly eventService = inject(EventService);
  readonly auth = inject(AuthService);

  readonly stepLabels = ['Planificación', 'Preparación', 'Confirmación', 'Cierre'];
  readonly pageSize = 5;
  currentPage = 1;
  showNewActModal = false;
  openDropdown: string | null = null;
  filterType = '';
  filterStatus = '';
  filterDate = '';
  searchQuery = '';
  loading = true;

  events: EventResponse[] = [];

  private readonly statusLabelMap: Record<string, string> = {
    'PLANIFICACION': 'Planificación',
    'PREPARACION': 'En preparación',
    'CONFIRMACION': 'Confirmación',
    'CERRADO': 'Cerrado',
  };

  private readonly stepIndexMap: Record<string, number> = {
    'PLANIFICACION': 1,
    'PREPARACION': 2,
    'CONFIRMACION': 3,
    'CERRADO': 4,
  };

  ngOnInit(): void {
    this.loadEvents();
  }

  get eventDates(): string[] {
    return this.events.map(e => e.eventDate);
  }

  get filteredActs(): EventResponse[] {
    return this.events.filter(event => {
      const matchesType = !this.filterType || event.eventType === this.filterType;
      const matchesStatus = !this.filterStatus || event.status === this.filterStatus;
      const matchesDate = !this.filterDate || event.eventDate === this.filterDate;
      const query = this.searchQuery.toLowerCase();
      const matchesSearch = !query || event.title.toLowerCase().includes(query)
        || event.reference.toLowerCase().includes(query)
        || (event.responsibleName || '').toLowerCase().includes(query)
        || event.eventType.toLowerCase().includes(query);
      return matchesType && matchesStatus && matchesDate && matchesSearch;
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredActs.length / this.pageSize));
  }

  get paginatedActs(): EventResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredActs.slice(start, start + this.pageSize);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get activeTypeLabel(): string {
    const map: Record<string, string> = {
      'CABILDO': 'Cabildo',
      'CULTOS': 'Cultos',
      'PROCESION': 'Procesión',
      'ENSAYO': 'Ensayo',
      'OTRO': 'Otro',
    };
    return this.filterType ? (map[this.filterType] || this.filterType) : 'Tipo de acto';
  }

  getEventTypeLabel(type: string): string {
    const map: Record<string, string> = {
      'CABILDO': 'Cabildo',
      'CULTOS': 'Cultos',
      'PROCESION': 'Procesión',
      'ENSAYO': 'Ensayo',
      'OTRO': 'Otro',
    };
    return map[type] || type;
  }

  get activeStatusLabel(): string {
    return this.filterStatus ? (this.statusLabelMap[this.filterStatus] || this.filterStatus) : 'Estado';
  }

  getStatusLabel(status: string): string {
    return this.statusLabelMap[status] || status;
  }

  getCurrentStep(status: string): number {
    return this.stepIndexMap[status] || 0;
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) {
      return '—';
    }
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  openNewActModal(): void {
    this.showNewActModal = true;
  }

  closeNewActModal(): void {
    this.showNewActModal = false;
  }

  onActCreated(event: EventResponse): void {
    this.showNewActModal = false;
    this.events = [event, ...this.events];
  }

  toggleDropdown(name: string): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectFilter(type: string, value: string): void {
    if (type === 'type') {
      this.filterType = value;
    } else if (type === 'status') {
      this.filterStatus = value;
    }
    this.currentPage = 1;
    this.openDropdown = null;
  }

  onSearch(query: string): void {
    this.searchQuery = sanitizeText(query);
    this.currentPage = 1;
  }

  onDateChange(date: string): void {
    this.filterDate = date;
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.filterType = '';
    this.filterStatus = '';
    this.filterDate = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
  }

  getStatusBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'PLANIFICACION': 'neutral',
      'PREPARACION': 'pending',
      'CONFIRMACION': 'confirmed',
      'CERRADO': 'neutral',
    };
    return variantMap[status] || 'neutral';
  }

  isStepDone(currentStep: number, stepIndex: number): boolean {
    return stepIndex < currentStep;
  }

  cloneAct(act: EventResponse): void {
    this.eventService.clone(act.id).subscribe({
      next: (cloned) => {
        this.events = [cloned, ...this.events];
      }
    });
  }

  private loadEvents(): void {
    this.loading = true;
    this.eventService.findAll().subscribe({
      next: (events) => {
        this.events = events;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
