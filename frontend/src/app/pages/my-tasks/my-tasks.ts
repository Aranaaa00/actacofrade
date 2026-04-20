import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Pagination } from '../../shared/components/pagination/pagination';
import { RejectModal } from '../../shared/components/reject-modal/reject-modal';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../services/auth.service';
import { MyTaskResponse, MyTaskStats } from '../../models/task.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';
import { getEventTypeLabel } from '../../shared/utils/label-maps.utils';
import { formatDateTime } from '../../shared/utils/date.utils';

const SIMPLIFIED_STATUS_LABELS: Record<string, string> = {
  'PLANNED': 'Pendiente',
  'ACCEPTED': 'Confirmada',
  'IN_PREPARATION': 'Confirmada',
  'CONFIRMED': 'Confirmada',
  'COMPLETED': 'Confirmada',
  'REJECTED': 'Rechazada',
};

const SIMPLIFIED_BADGE_VARIANTS: Record<string, string> = {
  'PLANNED': 'pending',
  'ACCEPTED': 'confirmed',
  'IN_PREPARATION': 'confirmed',
  'CONFIRMED': 'confirmed',
  'COMPLETED': 'confirmed',
  'REJECTED': 'rejected',
};

@Component({
  selector: 'app-my-tasks',
  imports: [RouterLink, FormsModule, LucideAngularModule, Badge, Pagination, RejectModal],
  templateUrl: './my-tasks.html',
})
export class MyTasks implements OnInit, OnDestroy {
  private readonly taskService = inject(TaskService);
  readonly auth = inject(AuthService);

  readonly pageSize = 5;
  currentPage = 1;
  totalPages = 1;
  loading = true;

  openDropdown: string | null = null;
  filterEventType = '';
  filterStatus = '';
  searchQuery = '';

  tasks: MyTaskResponse[] = [];
  stats: MyTaskStats = { pendingCount: 0, confirmedCount: 0, rejectedCount: 0 };

  showRejectModal = false;
  rejectingTask: MyTaskResponse | null = null;
  processingTaskId: number | null = null;

  private readonly searchSubject = new Subject<string>();
  private searchSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((query) => {
      this.searchQuery = sanitizeText(query);
      this.currentPage = 1;
      this.loadTasks();
    });
    this.loadData();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  get activeEventTypeLabel(): string {
    return this.filterEventType ? getEventTypeLabel(this.filterEventType) : 'Filtrar por acto';
  }

  get activeStatusLabel(): string {
    const labels: Record<string, string> = {
      'PENDING': 'Pendiente',
      'CONFIRMED': 'Confirmada',
      'REJECTED': 'Rechazada',
    };
    return this.filterStatus ? labels[this.filterStatus] || 'Filtrar por estado' : 'Filtrar por estado';
  }

  getEventTypeLabel = getEventTypeLabel;
  formatDateTime = formatDateTime;

  getSimplifiedStatusLabel(status: string): string {
    return SIMPLIFIED_STATUS_LABELS[status] || status;
  }

  getSimplifiedBadgeVariant(status: string): string {
    return SIMPLIFIED_BADGE_VARIANTS[status] || 'neutral';
  }

  toggleDropdown(name: string): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectFilter(type: string, value: string): void {
    if (type === 'eventType') {
      this.filterEventType = value;
    } else if (type === 'status') {
      this.filterStatus = value;
    }
    this.currentPage = 1;
    this.openDropdown = null;
    this.loadTasks();
  }

  onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  clearFilters(): void {
    this.filterEventType = '';
    this.filterStatus = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
    this.loadTasks();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.loadTasks();
    }
  }

  confirmTask(task: MyTaskResponse): void {
    this.processingTaskId = task.id;
    this.taskService.accept(task.eventId, task.id).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  openRejectModal(task: MyTaskResponse): void {
    this.rejectingTask = task;
    this.showRejectModal = true;
  }

  onRejectCancelled(): void {
    this.showRejectModal = false;
    this.rejectingTask = null;
  }

  onRejectConfirmed(reason: string): void {
    if (!this.rejectingTask) {
      return;
    }
    this.processingTaskId = this.rejectingTask.id;
    this.taskService.reject(this.rejectingTask.eventId, this.rejectingTask.id, sanitizeText(reason)).subscribe({
      next: () => {
        this.processingTaskId = null;
        this.showRejectModal = false;
        this.rejectingTask = null;
        this.loadData();
      },
      error: () => {
        this.processingTaskId = null;
      }
    });
  }

  private loadData(): void {
    this.loading = true;
    forkJoin({
      tasks: this.taskService.findMyTasks({
        eventType: this.filterEventType || undefined,
        statusGroup: this.filterStatus || undefined,
        search: this.searchQuery || undefined,
        page: this.currentPage - 1,
        size: this.pageSize
      }),
      stats: this.taskService.getMyTaskStats()
    }).subscribe({
      next: (data) => {
        this.tasks = data.tasks.content;
        this.totalPages = Math.max(1, data.tasks.totalPages);
        this.stats = data.stats;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private loadTasks(): void {
    this.taskService.findMyTasks({
      eventType: this.filterEventType || undefined,
      statusGroup: this.filterStatus || undefined,
      search: this.searchQuery || undefined,
      page: this.currentPage - 1,
      size: this.pageSize
    }).subscribe({
      next: (page) => {
        this.tasks = page.content;
        this.totalPages = Math.max(1, page.totalPages);
      }
    });
  }
}
