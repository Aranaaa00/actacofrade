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
import {
  getEventTypeLabel, getEventStatusLabel,
  getEventStatusBadgeVariant, getStepIndex
} from '../../shared/utils/label-maps.utils';
import { formatDate } from '../../shared/utils/date.utils';

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
    return this.filterType ? getEventTypeLabel(this.filterType) : 'Tipo de acto';
  }

  getEventTypeLabel = getEventTypeLabel;

  get activeStatusLabel(): string {
    return this.filterStatus ? getEventStatusLabel(this.filterStatus) : 'Estado';
  }

  getStatusLabel = getEventStatusLabel;

  getCurrentStep(status: string): number {
    return getStepIndex(status);
  }

  formatDate = formatDate;

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

  getStatusBadgeVariant = getEventStatusBadgeVariant;

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
