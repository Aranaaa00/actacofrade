export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  roleCode: string;
  hermandadNombre: string;
}

export interface AuthResponse {
  userId: number;
  token: string;
  email: string;
  fullName: string;
  roles: string[];
  hermandadNombre?: string;
}
