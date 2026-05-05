// Status of an admin change request.
export type AdminChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// Public view of a request (mirrors backend AdminChangeRequestResponse).
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

// Body sent when creating a new request.
export interface AdminChangeRequestCreate {
  message: string;
}

// Body sent when approving a request.
export interface AdminChangeRequestApprove {
  newAdminUserId: number;
}
