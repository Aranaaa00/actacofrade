import { SupportRequestType } from '../../models/admin-change-request.model';

/**
 * SuperAdmin-facing configuration for each support request type.
 * Drives icon, header, message section labels and resolution action verbs
 * so the detail view stays scalable: adding a new flow only requires a new entry.
 */
export interface SupportRequestTypeConfig {
  key: SupportRequestType;
  label: string;
  shortLabel: string;
  icon: string;
  accent: 'admin' | 'security' | 'verification' | 'contact';
  detailTitle: string;
  detailSummary: string;
  messageHeading: string;
  primaryActionLabel: string;
  primaryActionAriaLabel: string;
  requiresCandidate: boolean;
}

const ENTRIES: readonly SupportRequestTypeConfig[] = [
  {
    key: 'ADMIN_CHANGE',
    label: 'Cambio de administrador',
    shortLabel: 'Cambio admin',
    icon: 'user-cog',
    accent: 'admin',
    detailTitle: 'Solicitud de cambio de administrador',
    detailSummary: 'La hermandad pide reasignar el rol de administrador a otro miembro activo.',
    messageHeading: 'Motivo del cambio',
    primaryActionLabel: 'Aprobar y asignar',
    primaryActionAriaLabel: 'Aprobar la solicitud y asignar el nuevo administrador',
    requiresCandidate: true,
  },
  {
    key: 'VERIFICATION',
    label: 'Verificación manual',
    shortLabel: 'Verificación',
    icon: 'badge-check',
    accent: 'verification',
    detailTitle: 'Solicitud de verificación manual',
    detailSummary: 'El usuario pide validar manualmente su identidad. Gestiona la verificación desde el Centro de Intervención.',
    messageHeading: 'Justificación del solicitante',
    primaryActionLabel: 'Marcar como resuelta',
    primaryActionAriaLabel: 'Marcar la solicitud de verificación como resuelta',
    requiresCandidate: false,
  },
  {
    key: 'CONTACT',
    label: 'Contacto con soporte',
    shortLabel: 'Soporte',
    icon: 'mail',
    accent: 'contact',
    detailTitle: 'Mensaje a soporte',
    detailSummary: 'Consulta libre dirigida al equipo de ActaCofrade.',
    messageHeading: 'Mensaje recibido',
    primaryActionLabel: 'Marcar como resuelta',
    primaryActionAriaLabel: 'Marcar el contacto con soporte como resuelto',
    requiresCandidate: false,
  },
];

const FALLBACK: SupportRequestTypeConfig = ENTRIES[0];

const BY_KEY: Readonly<Record<SupportRequestType, SupportRequestTypeConfig>> = ENTRIES
  .reduce((acc, entry) => ({ ...acc, [entry.key]: entry }), {} as Record<SupportRequestType, SupportRequestTypeConfig>);

export const SUPPORT_REQUEST_TYPES: readonly SupportRequestTypeConfig[] = ENTRIES;

export function supportRequestTypeConfig(type: string | null | undefined): SupportRequestTypeConfig {
  if (!type) {
    return FALLBACK;
  }
  return BY_KEY[type as SupportRequestType] ?? FALLBACK;
}
