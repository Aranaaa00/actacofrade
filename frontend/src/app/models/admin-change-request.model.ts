// Status of an admin change request.
export type AdminChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// Discriminator that identifies which support flow originated the request.
export type SupportRequestType = 'ADMIN_CHANGE' | 'VERIFICATION' | 'CONTACT';

// Public view of a request (mirrors backend AdminChangeRequestResponse).
export interface AdminChangeRequestResponse {
  id: number;
  type: SupportRequestType;
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
  type: SupportRequestType;
  message: string;
}

// Body sent when approving a request.
export interface AdminChangeRequestApprove {
  newAdminUserId: number;
}
