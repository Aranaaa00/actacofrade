export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  roles: string[];
  active: boolean;
  lastLogin: string | null;
}

export interface RoleStatsResponse {
  administradores: number;
  responsables: number;
  colaboradores: number;
  consulta: number;
}

export interface UserUpdateRequest {
  fullName?: string;
  email?: string;
  roleCode?: string;
}

export interface UserCreateRequest {
  fullName: string;
  email: string;
  password: string;
  roleCode: string;
}

