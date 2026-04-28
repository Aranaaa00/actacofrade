const EVENT_TYPE_LABELS: Record<string, string> = {
  'CABILDO': 'Cabildo',
  'CULTOS': 'Cultos',
  'PROCESION': 'Procesión',
  'ENSAYO': 'Ensayo',
  'OTRO': 'Otro',
};

const EVENT_STATUS_LABELS: Record<string, string> = {
  'PLANIFICACION': 'Planificación',
  'PREPARACION': 'En preparación',
  'CONFIRMACION': 'Confirmación',
  'CIERRE': 'En cierre',
  'CERRADO': 'Cerrado',
};

const EVENT_STATUS_BADGE_VARIANTS: Record<string, string> = {
  'PLANIFICACION': 'neutral',
  'PREPARACION': 'pending',
  'CONFIRMACION': 'confirmed',
  'CIERRE': 'wood',
  'CERRADO': 'neutral',
};

const TASK_STATUS_LABELS: Record<string, string> = {
  'PLANNED': 'Planificada',
  'ACCEPTED': 'Aceptada',
  'IN_PREPARATION': 'En preparación',
  'CONFIRMED': 'Confirmada',
  'COMPLETED': 'Completada',
  'REJECTED': 'Rechazada',
};

const TASK_BADGE_VARIANTS: Record<string, string> = {
  'PLANNED': 'pending',
  'ACCEPTED': 'pending',
  'IN_PREPARATION': 'pending',
  'CONFIRMED': 'confirmed',
  'COMPLETED': 'confirmed',
  'REJECTED': 'rejected',
};

const DECISION_STATUS_LABELS: Record<string, string> = {
  'ACCEPTED': 'Aceptada',
  'PENDING': 'Pendiente',
  'REJECTED': 'Rechazada',
};

const DECISION_BADGE_VARIANTS: Record<string, string> = {
  'ACCEPTED': 'confirmed',
  'PENDING': 'pending',
  'REJECTED': 'rejected',
};

const INCIDENT_STATUS_LABELS: Record<string, string> = {
  'ABIERTA': 'Abierta',
  'RESUELTA': 'Resuelta',
};

const INCIDENT_BADGE_VARIANTS: Record<string, string> = {
  'ABIERTA': 'pending',
  'RESUELTA': 'confirmed',
};

const AREA_LABELS: Record<string, string> = {
  'MAYORDOMIA': 'Mayordomía',
  'SECRETARIA': 'Secretaría',
  'PRIOSTIA': 'Priostía',
  'TESORERIA': 'Tesorería',
  'DIPUTACION_MAYOR': 'Diputación Mayor',
};

const HISTORY_ACTION_LABELS: Record<string, string> = {
  'TASK_CREATED': 'Tarea creada',
  'TASK_ACCEPTED': 'Tarea aceptada',
  'TASK_IN_PREPARATION': 'Tarea en preparación',
  'TASK_CONFIRMED': 'Tarea confirmada',
  'TASK_COMPLETED': 'Tarea completada',
  'TASK_REJECTED': 'Tarea rechazada',
  'TASK_RESET': 'Tarea reiniciada',
  'TASK_UPDATED': 'Tarea actualizada',
  'TASK_DELETED': 'Tarea eliminada',
  'DECISION_CREATED': 'Decisión creada',
  'DECISION_ACCEPTED': 'Decisión aceptada',
  'DECISION_REJECTED': 'Decisión rechazada',
  'INCIDENT_CREATED': 'Incidencia creada',
  'INCIDENT_RESOLVED': 'Incidencia resuelta',
  'EVENT_CLOSED': 'Acto cerrado',
};

const ENTITY_TYPE_LABELS: Record<string, string> = {
  'TASK': 'Tarea',
  'DECISION': 'Decisión',
  'INCIDENT': 'Incidencia',
  'EVENT': 'Acto',
};

const HISTORY_BADGE_VARIANTS: Record<string, string> = {
  'TASK': 'pending',
  'DECISION': 'confirmed',
  'INCIDENT': 'rejected',
  'EVENT': 'wood',
};

const STEP_LABELS: Record<string, string> = {
  'PLANIFICACION': 'Planificación',
  'PREPARACION': 'Preparación',
  'CONFIRMACION': 'Confirmación',
  'CIERRE': 'Cierre',
};

const STEP_INDEX_MAP: Record<string, number> = {
  'PLANIFICACION': 1,
  'PREPARACION': 2,
  'CONFIRMACION': 3,
  'CIERRE': 4,
  'CERRADO': 4,
};

export function getEventTypeLabel(type: string): string {
  return EVENT_TYPE_LABELS[type] || type;
}

export function getEventStatusLabel(status: string): string {
  return EVENT_STATUS_LABELS[status] || status;
}

export function getEventStatusBadgeVariant(status: string): string {
  return EVENT_STATUS_BADGE_VARIANTS[status] || 'neutral';
}

export function getTaskStatusLabel(status: string): string {
  return TASK_STATUS_LABELS[status] || status;
}

export function getTaskBadgeVariant(status: string): string {
  return TASK_BADGE_VARIANTS[status] || 'neutral';
}

export function getDecisionStatusLabel(status: string): string {
  return DECISION_STATUS_LABELS[status] || status;
}

export function getDecisionBadgeVariant(status: string): string {
  return DECISION_BADGE_VARIANTS[status] || 'neutral';
}

export function getIncidentStatusLabel(status: string): string {
  return INCIDENT_STATUS_LABELS[status] || status;
}

export function getIncidentBadgeVariant(status: string): string {
  return INCIDENT_BADGE_VARIANTS[status] || 'neutral';
}

export function getAreaLabel(area: string): string {
  return AREA_LABELS[area] || area;
}

export function getActionLabel(action: string): string {
  return HISTORY_ACTION_LABELS[action] || action;
}

export function getEntityTypeLabel(entityType: string): string {
  return ENTITY_TYPE_LABELS[entityType] || entityType;
}

export function getHistoryBadgeVariant(entityType: string): string {
  return HISTORY_BADGE_VARIANTS[entityType] || 'neutral';
}

export function getStepLabel(key: string): string {
  return STEP_LABELS[key] || key;
}

export function getStepIndex(status: string): number {
  return STEP_INDEX_MAP[status] || 0;
}

const SIMPLIFIED_TASK_STATUS_LABELS: Record<string, string> = {
  'PLANNED': 'Pendiente',
  'ACCEPTED': 'Confirmada',
  'IN_PREPARATION': 'Confirmada',
  'CONFIRMED': 'Confirmada',
  'COMPLETED': 'Completada',
  'REJECTED': 'Rechazada',
};

const SIMPLIFIED_TASK_BADGE_VARIANTS: Record<string, string> = {
  'PLANNED': 'pending',
  'ACCEPTED': 'confirmed',
  'IN_PREPARATION': 'confirmed',
  'CONFIRMED': 'confirmed',
  'COMPLETED': 'confirmed',
  'REJECTED': 'rejected',
};

export function getSimplifiedTaskStatusLabel(status: string): string {
  return SIMPLIFIED_TASK_STATUS_LABELS[status] || status;
}

export function getSimplifiedTaskBadgeVariant(status: string): string {
  return SIMPLIFIED_TASK_BADGE_VARIANTS[status] || 'neutral';
}

export const EVENT_TYPE_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'CABILDO', label: 'Cabildo' },
  { value: 'CULTOS', label: 'Cultos' },
  { value: 'PROCESION', label: 'Procesión' },
  { value: 'ENSAYO', label: 'Ensayo' },
  { value: 'OTRO', label: 'Otro' },
];

export const EVENT_STATUS_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'PLANIFICACION', label: 'Planificación' },
  { value: 'PREPARACION', label: 'En preparación' },
  { value: 'CONFIRMACION', label: 'Confirmación' },
  { value: 'CIERRE', label: 'En cierre' },
];
