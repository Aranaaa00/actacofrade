import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { Pagination } from '../../shared/components/pagination/pagination';
import { FilterDropdown } from '../../shared/components/filter-dropdown/filter-dropdown';
import { ActEditor } from '../act-editor/act-editor';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { EventResponse } from '../../models/event.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';
import {
  getEventTypeLabel, getEventStatusLabel,
  getEventStatusBadgeVariant, getStepIndex,
  EVENT_TYPE_OPTIONS, EVENT_STATUS_OPTIONS
} from '../../shared/utils/label-maps.utils';
import { formatDate } from '../../shared/utils/date.utils';

@Component({
  selector: 'app-act-list',
  imports: [RouterLink, Badge, LucideAngularModule, Datepicker, ActEditor, Pagination, FilterDropdown],
  templateUrl: './act-list.html',
})
export class ActList implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  readonly auth = inject(AuthService);

  readonly stepLabels = ['Planificación', 'Preparación', 'Confirmación', 'Cierre'];
  readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  readonly eventStatusOptions = EVENT_STATUS_OPTIONS;
  readonly pageSize = 5;
  currentPage = 1;
  totalPages = 1;
  showNewActModal = false;
  openDropdown: string | null = null;
  filterType = '';
  filterStatus = '';
  filterDate = '';
  searchQuery = '';
  loading = true;
  availableDates: string[] = [];

  events: EventResponse[] = [];

  private readonly searchSubject = new Subject<string>();
  private searchSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((query) => {
      this.searchQuery = sanitizeText(query);
      this.currentPage = 1;
      this.loadEvents();
    });
    this.loadAvailableDates();
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
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
      this.loadEvents();
    }
  }

  openNewActModal(): void {
    this.showNewActModal = true;
  }

  closeNewActModal(): void {
    this.showNewActModal = false;
  }

  onActCreated(): void {
    this.showNewActModal = false;
    this.loadAvailableDates();
    this.loadEvents();
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
    this.loadEvents();
  }

  onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  onDateChange(date: string): void {
    this.filterDate = date;
    this.currentPage = 1;
    this.loadEvents();
  }

  clearFilters(): void {
    this.filterType = '';
    this.filterStatus = '';
    this.filterDate = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
    this.loadEvents();
  }

  getStatusBadgeVariant = getEventStatusBadgeVariant;

  isStepDone(currentStep: number, stepIndex: number): boolean {
    return stepIndex < currentStep;
  }

  cloneAct(act: EventResponse): void {
    this.eventService.clone(act.id).subscribe({
      next: () => {
        this.loadAvailableDates();
        this.loadEvents();
      }
    });
  }

  private loadAvailableDates(): void {
    this.eventService.availableDates().subscribe({
      next: (dates) => { this.availableDates = dates; }
    });
  }

  private loadEvents(): void {
    this.loading = true;
    this.eventService.filter({
      eventType: this.filterType || undefined,
      status: this.filterStatus || undefined,
      eventDate: this.filterDate || undefined,
      search: this.searchQuery || undefined,
      page: this.currentPage - 1,
      size: this.pageSize
    }).subscribe({
      next: (page) => {
        this.events = page.content;
        this.totalPages = Math.max(1, page.totalPages);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
