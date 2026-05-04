import { Component, OnInit, OnDestroy, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { Pagination } from '../../shared/components/pagination/pagination';
import { FilterDropdown } from '../../shared/components/filter-dropdown/filter-dropdown';
import { EditUserModal } from '../../shared/components/edit-user-modal/edit-user-modal';
import { Register } from '../register/register';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { RoleStatsResponse, UserResponse } from '../../models/user.model';
import { sanitizeText } from '../../shared/utils/sanitize.utils';
import { extractErrorMessage } from '../../shared/utils/http-error.utils';
import { ROLE_BADGE_VARIANTS, ROLE_LABELS, ROLES_ALL } from '../../shared/constants/roles.const';

interface PermissionColumn {
  key: string;
  label: string;
  roles: string[];
}

const PERMISSION_COLUMNS: PermissionColumn[] = [
  { key: 'manageUsers', label: 'Gestionar usuarios', roles: ['ADMINISTRADOR'] },
  { key: 'manageActs', label: 'Gestionar actos', roles: ['ADMINISTRADOR', 'RESPONSABLE'] },
  { key: 'createContent', label: 'Crear contenido', roles: ['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR'] },
  { key: 'consult', label: 'Consultar', roles: ['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA'] },
];

@Component({
  selector: 'app-users',
  imports: [FormsModule, LucideAngularModule, Badge, Pagination, FilterDropdown, EditUserModal, Register],
  templateUrl: './users.html',
})
export class Users implements OnInit, OnDestroy {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly pageSize = 5;
  readonly permissionColumns = PERMISSION_COLUMNS;

  readonly roleOptions = [
    { value: '', label: 'Todos' },
    ...ROLES_ALL.map((code) => ({ value: code, label: ROLE_LABELS[code] })),
  ];

  readonly statusOptions = [
    { value: '', label: 'Todos' },
    { value: 'ACTIVE', label: 'Activo' },
    { value: 'INACTIVE', label: 'Inactivo' },
  ];

  readonly editableRoles = this.roleOptions.filter((o) => o.value !== '');

  loading = true;
  errorMessage = '';

  users: UserResponse[] = [];
  stats: RoleStatsResponse = { administradores: 0, responsables: 0, colaboradores: 0, consulta: 0 };

  openDropdown: string | null = null;
  filterRole = '';
  filterStatus = '';
  searchQuery = '';
  currentPage = 1;
  processingUserId: number | null = null;

  showEditModal = false;
  editingUser: UserResponse | null = null;

  showCreateModal = false;

  private readonly searchSubject = new Subject<string>();
  private searchSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((query) => {
      this.searchQuery = sanitizeText(query);
      this.currentPage = 1;
    });
    this.loadData();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  get filteredUsers(): UserResponse[] {
    const search = this.searchQuery.trim().toLowerCase();
    return this.users.filter((u) => {
      const matchesRole = !this.filterRole || u.roles.includes(this.filterRole);
      const matchesStatus =
        !this.filterStatus ||
        (this.filterStatus === 'ACTIVE' && u.active) ||
        (this.filterStatus === 'INACTIVE' && !u.active);
      const matchesSearch =
        !search ||
        u.fullName.toLowerCase().includes(search) ||
        u.email.toLowerCase().includes(search);
      return matchesRole && matchesStatus && matchesSearch;
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredUsers.length / this.pageSize));
  }

  get pageUsers(): UserResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredUsers.slice(start, start + this.pageSize);
  }

  get activeRoleLabel(): string {
    return this.filterRole ? ROLE_LABELS[this.filterRole] : 'Rol';
  }

  get activeStatusLabel(): string {
    let label = 'Estado';
    if (this.filterStatus === 'ACTIVE') {
      label = 'Activo';
    } else if (this.filterStatus === 'INACTIVE') {
      label = 'Inactivo';
    }
    return label;
  }

  toggleDropdown(name: string): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectFilter(type: string, value: string): void {
    if (type === 'role') {
      this.filterRole = value;
    } else if (type === 'status') {
      this.filterStatus = value;
    }
    this.currentPage = 1;
    this.openDropdown = null;
  }

  onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  clearFilters(): void {
    this.filterRole = '';
    this.filterStatus = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  toggleActive(user: UserResponse): void {
    this.processingUserId = user.id;
    this.userService.toggleActive(user.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.users = this.users.map((u) => (u.id === updated.id ? updated : u));
        this.processingUserId = null;
      },
      error: (err) => {
        this.errorMessage = extractErrorMessage(err, 'No se pudo actualizar el estado del usuario.');
        this.processingUserId = null;
      },
    });
  }

  openEdit(user: UserResponse): void {
    this.editingUser = user;
    this.showEditModal = true;
  }

  onEditCancelled(): void {
    this.showEditModal = false;
    this.editingUser = null;
  }

  onEditConfirmed(roleCode: string): void {
    if (!this.editingUser) {
      return;
    }
    const id = this.editingUser.id;
    this.processingUserId = id;
    this.userService.update(id, { roleCode }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.users = this.users.map((u) => (u.id === updated.id ? updated : u));
        this.processingUserId = null;
        this.showEditModal = false;
        this.editingUser = null;
        this.refreshStats();
      },
      error: (err) => {
        this.errorMessage = extractErrorMessage(err, 'No se pudo actualizar el rol del usuario.');
        this.processingUserId = null;
      },
    });
  }

  openCreate(): void {
    this.showCreateModal = true;
  }

  onCreateClosed(): void {
    this.showCreateModal = false;
  }

  onUserCreated(user: UserResponse): void {
    this.users = [...this.users, user];
    this.showCreateModal = false;
    this.refreshStats();
  }

  deleteUser(user: UserResponse): void {
    this.processingUserId = user.id;
    this.userService.delete(user.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.users = this.users.filter((u) => u.id !== user.id);
        this.processingUserId = null;
        this.refreshStats();
        if (this.currentPage > this.totalPages) {
          this.currentPage = this.totalPages;
        }
      },
      error: (err) => {
        this.errorMessage = extractErrorMessage(err, 'No se pudo eliminar el usuario.');
        this.processingUserId = null;
      },
    });
  }

  isCurrentUser(user: UserResponse): boolean {
    return this.authService.getUserId() === user.id;
  }

  getRoleLabel(code: string): string {
    return ROLE_LABELS[code] ?? code;
  }

  getRoleBadgeVariant(code: string): string {
    return ROLE_BADGE_VARIANTS[code] ?? 'neutral';
  }

  getPrimaryRole(user: UserResponse): string {
    return user.roles[0] ?? '';
  }

  hasPermission(user: UserResponse, column: PermissionColumn): boolean {
    return user.roles.some((role) => column.roles.includes(role));
  }

  formatLastLogin(date: string | null): string {
    let formatted = '—';
    if (date) {
      const d = new Date(date);
      const day = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year = d.getFullYear();
      formatted = `${day}/${month}/${year}`;
    }
    return formatted;
  }

  private loadData(): void {
    this.loading = true;
    forkJoin({
      users: this.userService.findAll(),
      stats: this.userService.getStats(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.users = data.users;
        this.stats = data.stats;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = extractErrorMessage(err, 'No se pudieron cargar los usuarios. Inténtalo de nuevo más tarde.');
        this.loading = false;
      },
    });
  }

  private refreshStats(): void {
    this.userService.getStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (stats) => {
        this.stats = stats;
      },
    });
  }
}
