// Application role identifiers used by guards and the auth service.
export type Role = 'ADMINISTRADOR' | 'RESPONSABLE' | 'COLABORADOR' | 'CONSULTA';

// All known roles. Useful as a default for read-only screens.
export const ROLES_ALL: readonly Role[] = ['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA'];
// Roles allowed to manage acts (create, edit, close).
export const ROLES_MANAGE: readonly Role[] = ['ADMINISTRADOR', 'RESPONSABLE'];
// Roles allowed to perform write operations on tasks/decisions/incidents.
export const ROLES_WRITE: readonly Role[] = ['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR'];
// Roles with full administrative capabilities.
export const ROLES_ADMIN: readonly Role[] = ['ADMINISTRADOR'];

// Human-readable role labels rendered in the UI.
export const ROLE_LABELS: Readonly<Record<string, string>> = {
  ADMINISTRADOR: 'Administrador',
  RESPONSABLE: 'Responsable',
  COLABORADOR: 'Colaborador',
  CONSULTA: 'Consulta',
};

// Badge style applied to each role chip.
export const ROLE_BADGE_VARIANTS: Readonly<Record<string, string>> = {
  ADMINISTRADOR: 'confirmed',
  RESPONSABLE: 'wood',
  COLABORADOR: 'neutral',
  CONSULTA: 'neutral',
};
