import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Badge } from '../../../shared/components/badge/badge';
import { AdminChangeRequestResponse } from '../../../models/admin-change-request.model';
import { formatDateTime } from '../../../shared/utils/date.utils';
import { adminRequestStatusLabel, adminRequestStatusVariant } from '../admin-request-status.util';

// Atomic list of admin change requests; emits the selected item to the parent.
@Component({
  selector: 'app-request-list',
  imports: [Badge],
  templateUrl: './request-list.html',
})
export class RequestList {
  @Input() requests: AdminChangeRequestResponse[] = [];
  @Input() selectedId: number | null = null;
  @Output() selected = new EventEmitter<AdminChangeRequestResponse>();

  trackById = (_: number, item: AdminChangeRequestResponse): number => item.id;

  statusLabel(status: string): string {
    return adminRequestStatusLabel(status);
  }

  statusVariant(status: string): string {
    return adminRequestStatusVariant(status);
  }

  formattedDate(value: string): string {
    return formatDateTime(value);
  }

  onSelect(item: AdminChangeRequestResponse): void {
    this.selected.emit(item);
  }
}
