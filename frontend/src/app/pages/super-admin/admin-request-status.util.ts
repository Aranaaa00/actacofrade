import { AdminChangeRequestStatus } from '../../models/admin-change-request.model';

// Spanish labels shown for each request status (used by list and detail).
export const ADMIN_REQUEST_STATUS_LABELS: Readonly<Record<AdminChangeRequestStatus, string>> = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobada',
  REJECTED: 'Rechazada',
};

// Badge variants per status, aligned with the shared Badge component tokens.
export const ADMIN_REQUEST_STATUS_VARIANTS: Readonly<Record<AdminChangeRequestStatus, string>> = {
  PENDING: 'wood',
  APPROVED: 'confirmed',
  REJECTED: 'neutral',
};

// Returns the readable label, or the raw code if unknown.
export function adminRequestStatusLabel(status: string): string {
  return ADMIN_REQUEST_STATUS_LABELS[status as AdminChangeRequestStatus] ?? status;
}

// Returns the visual variant, or 'neutral' if unknown.
export function adminRequestStatusVariant(status: string): string {
  return ADMIN_REQUEST_STATUS_VARIANTS[status as AdminChangeRequestStatus] ?? 'neutral';
}
