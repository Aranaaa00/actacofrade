import { SupportRequestType } from '../../models/admin-change-request.model';

// PASSWORD_RESET is a UI-only key used by the contact modal to trigger the
// self-service flow; it never reaches the AdminChangeRequest backend type.
export type SupportCategoryKey = SupportRequestType | 'PASSWORD_RESET';

export interface SupportCategory {
  key: SupportCategoryKey;
  label: string;
  icon: string;
  prefix: string;
  description: string;
  placeholder: string;
}

export const SUPPORT_CATEGORIES: readonly SupportCategory[] = [
  {
    key: 'ADMIN_CHANGE',
    label: 'Cambio de administrador',
    icon: 'user-cog',
    prefix: '[Cambio de administrador]',
    description: 'Solicita al equipo de ActaCofrade un cambio del administrador actual de tu hermandad.',
    placeholder: 'Explica brevemente por qué necesitas un cambio de administrador en tu hermandad.',
  },
  {
    key: 'PASSWORD_RESET',
    label: 'Recuperar contraseña',
    icon: 'key-round',
    prefix: '[Recuperación de contraseña]',
    description: 'Solicita el restablecimiento de tu contraseña. Recibirás un enlace en tu correo para crear una nueva.',
    placeholder: 'Indica el motivo de la solicitud (por ejemplo: no recuerdo la contraseña, accesos comprometidos, etc.).',
  },
  {
    key: 'VERIFICATION',
    label: 'Pedir verificación',
    icon: 'badge-check',
    prefix: '[Verificación manual]',
    description: 'Pide la verificación manual de tu cuenta. Aporta los datos o motivos que avalen tu identidad.',
    placeholder: 'Explica brevemente por qué solicitas la verificación manual y aporta referencias si las tienes.',
  },
  {
    key: 'CONTACT',
    label: 'Contactar con soporte',
    icon: 'mail',
    prefix: '[Contacto general]',
    description: 'Envía un mensaje libre al SuperAdmin para cualquier otra cuestión de soporte.',
    placeholder: 'Escribe tu consulta o petición.',
  },
];
