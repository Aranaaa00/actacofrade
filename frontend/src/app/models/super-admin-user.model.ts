// Modelos TS espejo de los DTOs del backend para el Centro de Intervención.

export type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'BANNED';

export interface SuperAdminUserResponse {
  id: number;
  fullName: string;
  email: string;
  roles: string[];
  status: AccountStatus;
  statusReason: string | null;
  statusChangedAt: string | null;
  manuallyVerified: boolean;
  manuallyVerifiedAt: string | null;
  hermandadNombre: string | null;
  lastLogin: string | null;
  createdAt: string;
}

export interface SuperAdminStatusRequest {
  status: AccountStatus;
  reason?: string | null;
}

export interface SuperAdminRoleRequest {
  roleCode: 'ADMINISTRADOR' | 'RESPONSABLE' | 'COLABORADOR' | 'CONSULTA';
  reason: string;
}

export interface InterventionLogEntry {
  id: number;
  action: string;
  targetUserId: number | null;
  actorId: number | null;
  actorName: string | null;
  performedAt: string;
  details: string | null;
  changes: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}
