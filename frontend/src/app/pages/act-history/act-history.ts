import { Component, OnInit, OnDestroy, HostListener, ElementRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { FilterDropdown, FilterOption } from '../../shared/components/filter-dropdown/filter-dropdown';
import { Pagination } from '../../shared/components/pagination/pagination';
import { EventService } from '../../services/event.service';
import { UserService } from '../../services/user.service';
import { EventResponse } from '../../models/event.model';
import { UserResponse } from '../../models/user.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';
import {
  getEventTypeLabel,
  getEventStatusLabel,
  getEventStatusBadgeVariant,
  EVENT_TYPE_OPTIONS
} from '../../shared/utils/label-maps.utils';

interface DateGroup {
  date: string;
  events: EventResponse[];
}

@Component({
  selector: 'app-act-history',
  imports: [RouterLink, LucideAngularModule, Badge, Datepicker, FilterDropdown, Pagination],
  templateUrl: './act-history.html',
})
export class ActHistory implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  private readonly userService = inject(UserService);
  private readonly el = inject(ElementRef);

  readonly pageSize = 5;
  readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  currentPage = 1;
  totalPages = 1;

  filterType = '';
  filterResponsibleId: number | null = null;
  filterDateFrom = '';
  filterDateTo = '';
  searchQuery = '';

  openDropdown: string | null = null;
  events: EventResponse[] = [];
  users: UserResponse[] = [];
  availableDates: string[] = [];
  loading = true;

  private readonly searchSubject = new Subject<string>();
  private searchSubscription: Subscription | null = null;

  ngOnInit(): void {
    // debounce keystrokes so we only request a new page after the user stops typing
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((query) => {
      this.searchQuery = sanitizeText(query);
      this.currentPage = 1;
      this.loadEvents();
    });

    // load the dropdown sources in parallel and then fetch the first page
    forkJoin({
      users: this.userService.findAll(),
      dates: this.eventService.availableDates()
    }).subscribe({
      next: ({ users, dates }) => {
        this.users = users;
        this.availableDates = dates;
        this.loadEvents();
      },
      error: () => {
        // keep history usable even when filter data fails to load
        this.users = [];
        this.availableDates = [];
        this.loadEvents();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  get groupedEvents(): DateGroup[] {
    // group every event by date so the template can render one section per day
    const groups: Map<string, EventResponse[]> = new Map();
    for (const event of this.events) {
      const key = event.eventDate as string;
      if (!groups.has(key)) { groups.set(key, []); }
      groups.get(key)!.push(event);
    }
    return Array.from(groups.entries()).map(([date, evts]) => ({ date, events: evts }));
  }

  get activeTypeLabel(): string {
    return this.filterType ? getEventTypeLabel(this.filterType) : 'Tipo de acto';
  }

  get activeResponsibleLabel(): string {
    if (!this.filterResponsibleId) { return 'Responsable'; }
    const user = this.users.find(u => u.id === this.filterResponsibleId);
    return user ? user.fullName : 'Responsable';
  }

  get activeDateRangeLabel(): string {
    if (!this.filterDateFrom && !this.filterDateTo) { return 'Fechas'; }
    const parts: string[] = [];
    if (this.filterDateFrom) { parts.push(this.formatShortDate(this.filterDateFrom)); }
    if (this.filterDateTo) { parts.push(this.formatShortDate(this.filterDateTo)); }
    return parts.join(' – ');
  }

  get responsibleOptions(): FilterOption[] {
    return [
      { value: '', label: 'Todos' },
      ...this.users.map(u => ({ value: String(u.id), label: u.fullName }))
    ];
  }

  getEventTypeLabel = getEventTypeLabel;
  getStatusLabel = getEventStatusLabel;
  getStatusBadgeVariant = getEventStatusBadgeVariant;

  isClosed(event: EventResponse): boolean {
    return event.status === 'CLOSED';
  }

  formatTimelineDate(dateStr: string): string {
    const d = new Date(dateStr + 'T00:00:00');
    const day = d.getDate();
    const month = d.toLocaleDateString('es-ES', { month: 'short' }).toUpperCase().replace('.', '');
    const year = d.getFullYear();
    return `${day} ${month} ${year}`;
  }

  private formatShortDate(dateStr: string): string {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' }).toUpperCase();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.loadEvents();
    }
  }

  toggleDropdown(name: string): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectFilter(type: string, value: string): void {
    if (type === 'type') {
      this.filterType = value;
    } else if (type === 'responsible') {
      this.filterResponsibleId = value ? Number(value) : null;
    }
    this.currentPage = 1;
    this.openDropdown = null;
    this.loadEvents();
  }

  onDateFromChange(date: string): void {
    this.filterDateFrom = date;
    if (this.filterDateTo && this.filterDateFrom > this.filterDateTo) {
      this.filterDateTo = '';
    }
    this.currentPage = 1;
    this.loadEvents();
  }

  onDateToChange(date: string): void {
    this.filterDateTo = date;
    if (this.filterDateFrom && this.filterDateTo < this.filterDateFrom) {
      this.filterDateTo = '';
    } else if (this.filterDateFrom && this.filterDateTo && !this.rangeContainsAvailableDate()) {
      this.filterDateTo = '';
    }
    this.currentPage = 1;
    this.loadEvents();
  }

  private rangeContainsAvailableDate(): boolean {
    return this.availableDates.some(d => d >= this.filterDateFrom && d <= this.filterDateTo);
  }

  onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  clearFilters(): void {
    this.filterType = '';
    this.filterResponsibleId = null;
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
    this.loadEvents();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target)) {
      this.openDropdown = null;
    }
  }

  private loadEvents(): void {
    this.loading = true;
    this.eventService.history({
      eventType: this.filterType || undefined,
      responsibleId: this.filterResponsibleId ?? undefined,
      dateFrom: this.filterDateFrom || undefined,
      dateTo: this.filterDateTo || undefined,
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
