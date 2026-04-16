export interface AuditLogResponse {
  id: number;
  eventId: number;
  entityType: string;
  entityId: number;
  action: string;
  performedById: number | null;
  performedByName: string | null;
  performedAt: string;
  details: string | null;
}

export interface AuditLogPage {
  content: AuditLogResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
