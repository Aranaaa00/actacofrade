import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../../shared/components/badge/badge';
import { AdminChangeRequestResponse } from '../../../models/admin-change-request.model';
import { UserResponse } from '../../../models/user.model';
import { formatDateTime } from '../../../shared/utils/date.utils';
import { adminRequestStatusLabel, adminRequestStatusVariant } from '../admin-request-status.util';
import { SupportRequestTypeConfig, supportRequestTypeConfig } from '../../../shared/constants/request-type.config';

// Atomic detail panel: renders dynamic content per request type and emits
// approve/resolve/reject events for the parent page to handle.
@Component({
  selector: 'app-request-detail',
  imports: [FormsModule, LucideAngularModule, Badge, RouterLink],
  templateUrl: './request-detail.html',
})
export class RequestDetail implements OnChanges {
  @Input() request: AdminChangeRequestResponse | null = null;
  @Input() candidates: UserResponse[] = [];
  @Input() processing = false;
  @Input() loadingCandidates = false;
  @Output() approved = new EventEmitter<number>();
  @Output() resolved = new EventEmitter<void>();
  @Output() rejected = new EventEmitter<void>();

  selectedCandidateId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['request']) {
      this.selectedCandidateId = null;
    }
  }

  get isPending(): boolean {
    return this.request?.status === 'PENDING';
  }

  get typeConfig(): SupportRequestTypeConfig {
    return supportRequestTypeConfig(this.request?.type);
  }

  get isAdminChange(): boolean {
    return this.typeConfig.requiresCandidate;
  }

  get canConfirm(): boolean {
    if (!this.isPending || this.processing) {
      return false;
    }
    return this.isAdminChange ? this.selectedCandidateId !== null : true;
  }

  statusLabel(status: string): string {
    return adminRequestStatusLabel(status);
  }

  statusVariant(status: string): string {
    return adminRequestStatusVariant(status);
  }

  formattedDate(value: string | null): string {
    return formatDateTime(value);
  }

  onConfirm(): void {
    if (!this.canConfirm) {
      return;
    }
    if (this.isAdminChange && this.selectedCandidateId !== null) {
      this.approved.emit(this.selectedCandidateId);
      return;
    }
    this.resolved.emit();
  }

  onReject(): void {
    this.rejected.emit();
  }
}
