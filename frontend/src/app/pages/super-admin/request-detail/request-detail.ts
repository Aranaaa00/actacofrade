import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../../shared/components/badge/badge';
import { AdminChangeRequestResponse } from '../../../models/admin-change-request.model';
import { UserResponse } from '../../../models/user.model';
import { formatDateTime } from '../../../shared/utils/date.utils';
import { adminRequestStatusLabel, adminRequestStatusVariant } from '../admin-request-status.util';

// Atomic detail panel: shows a request and emits approve/reject events.
@Component({
  selector: 'app-request-detail',
  imports: [FormsModule, LucideAngularModule, Badge],
  templateUrl: './request-detail.html',
})
export class RequestDetail implements OnChanges {
  @Input() request: AdminChangeRequestResponse | null = null;
  @Input() candidates: UserResponse[] = [];
  @Input() processing = false;
  @Input() loadingCandidates = false;
  @Output() approved = new EventEmitter<number>();
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

  statusLabel(status: string): string {
    return adminRequestStatusLabel(status);
  }

  statusVariant(status: string): string {
    return adminRequestStatusVariant(status);
  }

  formattedDate(value: string | null): string {
    return formatDateTime(value);
  }

  onApprove(): void {
    if (this.selectedCandidateId !== null) {
      this.approved.emit(this.selectedCandidateId);
    }
  }

  onReject(): void {
    this.rejected.emit();
  }
}
