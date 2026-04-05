export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  roles: string[];
  active: boolean;
  lastLogin: string | null;
}
