export type AdminChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AdminChangeRequestResponse {
  id: number;
  hermandadId: number;
  hermandadNombre: string;
  requesterUserId: number;
  requesterFullName: string;
  requesterEmail: string;
  message: string;
  status: AdminChangeRequestStatus;
  newAdminUserId: number | null;
  newAdminFullName: string | null;
  resolvedByUserId: number | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface AdminChangeRequestCreate {
  message: string;
}

export interface AdminChangeRequestApprove {
  newAdminUserId: number;
}
