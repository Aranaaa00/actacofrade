import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule } from 'lucide-angular';
import { Banner } from '../../shared/components/banner/banner';
import { RequestList } from './request-list/request-list';
import { RequestDetail } from './request-detail/request-detail';
import { AdminChangeRequestService } from '../../services/admin-change-request.service';
import { AdminChangeRequestResponse } from '../../models/admin-change-request.model';
import { UserResponse } from '../../models/user.model';
import { extractErrorMessage } from '../../shared/utils/http-error.utils';

// Super admin page: lists requests and handles approve/reject actions.
@Component({
  selector: 'app-super-admin',
  imports: [LucideAngularModule, Banner, RequestList, RequestDetail],
  templateUrl: './super-admin.html',
})
export class SuperAdmin implements OnInit {
  private readonly service = inject(AdminChangeRequestService);
  private readonly destroyRef = inject(DestroyRef);

  loading = true;
  loadingCandidates = false;
  processing = false;
  errorMessage = '';
  successMessage = '';

  requests: AdminChangeRequestResponse[] = [];
  selected: AdminChangeRequestResponse | null = null;
  candidates: UserResponse[] = [];

  ngOnInit(): void {
    this.loadRequests();
  }

  get pendingCount(): number {
    return this.requests.filter((r) => r.status === 'PENDING').length;
  }

  onSelectRequest(request: AdminChangeRequestResponse): void {
    this.selected = request;
    this.successMessage = '';
    this.errorMessage = '';
    if (request.status === 'PENDING') {
      this.loadCandidates(request.id);
    } else {
      this.candidates = [];
    }
  }

  onApprove(newAdminUserId: number): void {
    if (!this.selected || this.processing) {
      return;
    }
    const id = this.selected.id;
    this.processing = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.approve(id, { newAdminUserId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.processing = false;
          this.successMessage = 'Solicitud aprobada y administrador actualizado.';
          this.applyUpdated(updated);
        },
        error: (err) => {
          this.processing = false;
          this.errorMessage = extractErrorMessage(err, 'No se pudo aprobar la solicitud.');
        },
      });
  }

  onReject(): void {
    if (!this.selected || this.processing) {
      return;
    }
    const id = this.selected.id;
    this.processing = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.reject(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.processing = false;
          this.successMessage = 'Solicitud rechazada correctamente.';
          this.applyUpdated(updated);
        },
        error: (err) => {
          this.processing = false;
          this.errorMessage = extractErrorMessage(err, 'No se pudo rechazar la solicitud.');
        },
      });
  }

  private loadRequests(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.requests = items;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = extractErrorMessage(err, 'No se pudieron cargar las solicitudes.');
        },
      });
  }

  private loadCandidates(requestId: number): void {
    this.loadingCandidates = true;
    this.candidates = [];
    this.service.findCandidates(requestId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.candidates = items;
          this.loadingCandidates = false;
        },
        error: (err) => {
          this.loadingCandidates = false;
          this.errorMessage = extractErrorMessage(err, 'No se pudieron cargar los candidatos.');
        },
      });
  }

  private applyUpdated(updated: AdminChangeRequestResponse): void {
    this.requests = this.requests.map((r) => (r.id === updated.id ? updated : r));
    this.selected = updated;
    this.candidates = [];
  }
}
