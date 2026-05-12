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
  hasAvatar?: boolean;
}

// Returned by /register and /resend-verification: the response is intentionally
// generic to avoid leaking whether the email is already registered.
export interface RegistrationStatusResponse {
  status: 'pending_verification' | string;
  message: string;
}

export interface ResendVerificationRequest {
  email: string;
}
