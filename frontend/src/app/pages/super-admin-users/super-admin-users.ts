import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Observable } from 'rxjs';
import { SuperAdminUserService } from '../../services/super-admin-user.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import {
  AccountStatus,
  InterventionLogEntry,
  SuperAdminRoleRequest,
  SuperAdminUserResponse,
} from '../../models/super-admin-user.model';
import { ROLE_LABELS, Role } from '../../shared/constants/roles.const';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/components/form-field/form-field';

type AssignableRole = SuperAdminRoleRequest['roleCode'];

const ASSIGNABLE_ROLES: readonly AssignableRole[] = [
  'ADMINISTRADOR',
  'RESPONSABLE',
  'COLABORADOR',
  'CONSULTA',
];
const SUPER_ADMIN_ROLE: Role = 'SUPER_ADMIN';
const LIST_PAGE_SIZE = 50;
const LOG_PAGE_SIZE = 20;

type PendingAction =
  | { kind: 'status' }
  | { kind: 'role' }
  | { kind: 'password-reset' };

const STATUS_LABELS: Readonly<Record<AccountStatus, string>> = {
  ACTIVE: 'Activa',
  SUSPENDED: 'Suspendida',
  BANNED: 'Baneada',
};

const STATUS_BADGE_CLASSES: Readonly<Record<AccountStatus, string>> = {
  ACTIVE: 'badge--confirmed',
  SUSPENDED: 'badge--pending',
  BANNED: 'badge--rejected',
};

const ACTION_LABELS: Readonly<Record<string, string>> = {
  SUPERADMIN_STATUS_CHANGE: 'Cambio de estado',
  SUPERADMIN_MANUAL_VERIFY: 'Verificación manual',
  SUPERADMIN_MANUAL_UNVERIFY: 'Retirada de verificación',
  SUPERADMIN_ROLE_OVERRIDE: 'Cambio de rol',
  SUPERADMIN_PASSWORD_RESET: 'Reseteo de contraseña',
};

const CHANGE_FIELD_LABELS: Readonly<Record<string, string>> = {
  status: 'Estado',
  reason: 'Motivo',
  manuallyVerified: 'Verificación manual',
  roles: 'Rol',
};

const STATUS_VALUE_LABELS: Readonly<Record<string, string>> = {
  ACTIVE: 'Activa',
  SUSPENDED: 'Suspendida',
  BANNED: 'Baneada',
};

export interface ChangeRow {
  readonly label: string;
  readonly from: string;
  readonly to: string;
}

/**
 * Centro de Intervención del Super Administrador. Permite buscar usuarios y
 * ejecutar acciones manuales: cambiar estado, verificación, rol y reseteo de
 * contraseña. Todas las acciones quedan registradas en el log auditable.
 */
@Component({
  selector: 'app-super-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ConfirmDialog, FormField],
  templateUrl: './super-admin-users.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuperAdminUsers implements OnInit {
  private readonly service = inject(SuperAdminUserService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly roleLabels = ROLE_LABELS;
  readonly assignableRoles = ASSIGNABLE_ROLES;

  query = '';
  readonly loading = signal(false);
  readonly processing = signal(false);
  readonly users = signal<readonly SuperAdminUserResponse[]>([]);
  readonly selected = signal<SuperAdminUserResponse | null>(null);

  readonly logsLoading = signal(false);
  readonly logs = signal<readonly InterventionLogEntry[]>([]);

  statusForm: { status: AccountStatus; reason: string } = { status: 'ACTIVE', reason: '' };
  roleForm: { roleCode: AssignableRole; reason: string } = { roleCode: 'CONSULTA', reason: '' };

  readonly pending = signal<PendingAction | null>(null);

  readonly actorEmail = computed(() => this.auth.getUser()?.email?.toLowerCase() ?? '');

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.service
      .search(this.query.trim(), 0, LIST_PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.users.set(page.content);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.toast.fromHttpError(err, 'No se pudo cargar la lista de usuarios.');
        },
      });
  }

  select(user: SuperAdminUserResponse): void {
    this.selected.set(user);
    this.statusForm = { status: user.status, reason: '' };
    const firstAssignable = user.roles.find((r): r is AssignableRole =>
      (ASSIGNABLE_ROLES as readonly string[]).includes(r),
    );
    this.roleForm = { roleCode: firstAssignable ?? 'CONSULTA', reason: '' };
    this.loadLogs(user.id);
  }

  isSelf(user: SuperAdminUserResponse | null): boolean {
    return !!user && user.email.toLowerCase() === this.actorEmail();
  }

  isSuperAdmin(user: SuperAdminUserResponse | null): boolean {
    return !!user && user.roles.includes(SUPER_ADMIN_ROLE);
  }

  statusLabel(status: AccountStatus): string {
    return STATUS_LABELS[status];
  }

  statusBadgeClass(status: AccountStatus): string {
    return STATUS_BADGE_CLASSES[status];
  }

  actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action;
  }

  formatChanges(raw: string | null): readonly ChangeRow[] {
    if (!raw) {
      return [];
    }
    let parsed: unknown;
    try {
      parsed = JSON.parse(raw);
    } catch {
      return [];
    }
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return [];
    }
    const rows: ChangeRow[] = [];
    for (const [field, diff] of Object.entries(parsed as Record<string, unknown>)) {
      if (!diff || typeof diff !== 'object' || Array.isArray(diff)) {
        continue;
      }
      const d = diff as { oldValue?: unknown; newValue?: unknown };
      rows.push({
        label: CHANGE_FIELD_LABELS[field] ?? field,
        from: this.formatChangeValue(field, d.oldValue),
        to: this.formatChangeValue(field, d.newValue),
      });
    }
    return rows;
  }

  trackByChange(_: number, row: ChangeRow): string {
    return row.label;
  }

  private formatChangeValue(field: string, value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    const str = String(value);
    if (field === 'status') {
      return STATUS_VALUE_LABELS[str] ?? str;
    }
    if (field === 'manuallyVerified') {
      return str === 'true' ? 'Sí' : 'No';
    }
    if (field === 'roles') {
      return str.split(',').map((r) => this.roleLabels[r.trim()] ?? r.trim()).join(', ');
    }
    return str;
  }

  statusReasonLabel(status: AccountStatus): string {
    return status === 'ACTIVE' ? 'Motivo (opcional)' : 'Motivo *';
  }

  trackById(_: number, user: SuperAdminUserResponse): number {
    return user.id;
  }

  trackByLogId(_: number, entry: InterventionLogEntry): number {
    return entry.id;
  }

  // ───────── diálogo de confirmación ─────────

  requestStatus(): void {
    if (!this.canSubmitStatus()) {
      this.toast.error('Indica el motivo para suspender o banear la cuenta.');
      return;
    }
    this.pending.set({ kind: 'status' });
  }

  requestVerify(verify: boolean): void {
    const user = this.selected();
    if (!user) {
      return;
    }
    const obs = verify ? this.service.verify(user.id) : this.service.unverify(user.id);
    this.run(
      obs,
      verify ? 'Usuario verificado manualmente.' : 'Verificación manual retirada.',
      'No se pudo aplicar la verificación.',
    );
  }

  requestRole(): void {
    if (!this.roleForm.reason.trim()) {
      this.toast.error('Indica el motivo del cambio de rol.');
      return;
    }
    this.pending.set({ kind: 'role' });
  }

  requestPasswordReset(): void {
    this.pending.set({ kind: 'password-reset' });
  }

  cancelPending(): void {
    this.pending.set(null);
  }

  confirmPending(): void {
    const action = this.pending();
    if (!action) {
      return;
    }
    this.pending.set(null);
    switch (action.kind) {
      case 'status':
        this.executeStatus();
        return;
      case 'role':
        this.executeRole();
        return;
      case 'password-reset':
        this.executePasswordReset();
    }
  }

  pendingTitle(): string {
    const action = this.pending();
    if (!action) {
      return '';
    }
    switch (action.kind) {
      case 'status':
        return 'Confirmar cambio de estado';
      case 'role':
        return 'Confirmar cambio de rol';
      case 'password-reset':
        return 'Enviar correo de restablecimiento';
    }
  }

  pendingMessage(): string {
    const action = this.pending();
    const user = this.selected();
    if (!action || !user) {
      return '';
    }
    switch (action.kind) {
      case 'status':
        return `Vas a cambiar el estado de ${user.email} a ${this.statusLabel(this.statusForm.status)}.`;
      case 'role':
        return `Vas a forzar el rol ${this.roleLabels[this.roleForm.roleCode]} en ${user.email}.`;
      case 'password-reset':
        return `Se enviará un correo de restablecimiento a ${user.email}. El enlace caduca en 7 días.`;
    }
  }

  pendingVariant(): 'primary' | 'danger' {
    const action = this.pending();
    if (action?.kind === 'status' && this.statusForm.status !== 'ACTIVE') {
      return 'danger';
    }
    return 'primary';
  }

  // ───────── acciones ─────────

  private canSubmitStatus(): boolean {
    if (this.statusForm.status === 'ACTIVE') {
      return true;
    }
    return this.statusForm.reason.trim().length > 0;
  }

  private executeStatus(): void {
    const user = this.selected();
    if (!user) {
      return;
    }
    this.run(
      this.service.updateStatus(user.id, {
        status: this.statusForm.status,
        reason: this.statusForm.reason.trim() || null,
      }),
      'Estado actualizado correctamente.',
      'No se pudo cambiar el estado.',
    );
  }

  private executeRole(): void {
    const user = this.selected();
    if (!user) {
      return;
    }
    this.run(
      this.service.overrideRole(user.id, {
        roleCode: this.roleForm.roleCode,
        reason: this.roleForm.reason.trim(),
      }),
      'Rol actualizado correctamente.',
      'No se pudo cambiar el rol.',
    );
  }

  private executePasswordReset(): void {
    const user = this.selected();
    if (!user) {
      return;
    }
    this.processing.set(true);
    this.service
      .triggerPasswordReset(user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.processing.set(false);
          this.toast.success('Correo de restablecimiento enviado.');
          this.loadLogs(user.id);
        },
        error: (err) => {
          this.processing.set(false);
          this.toast.fromHttpError(err, 'No se pudo enviar el correo de restablecimiento.');
        },
      });
  }

  // ───────── helpers ─────────

  private run(
    request: Observable<SuperAdminUserResponse>,
    successMessage: string,
    errorFallback: string,
  ): void {
    this.processing.set(true);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.processing.set(false);
        this.toast.success(successMessage);
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.selected.set(updated);
        this.statusForm = { status: updated.status, reason: '' };
        this.loadLogs(updated.id);
      },
      error: (err) => {
        this.processing.set(false);
        this.toast.fromHttpError(err, errorFallback);
      },
    });
  }

  private loadLogs(userId: number): void {
    this.logsLoading.set(true);
    this.service
      .findUserLogs(userId, 0, LOG_PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.logs.set(page.content);
          this.logsLoading.set(false);
        },
        error: (err) => {
          this.logsLoading.set(false);
          this.logs.set([]);
          this.toast.fromHttpErrorSilencingAuth(
            err,
            'No se pudo cargar el historial de intervenciones.',
          );
        },
      });
  }
}
