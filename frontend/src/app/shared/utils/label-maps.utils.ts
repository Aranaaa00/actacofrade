// translation tables that turn backend enum codes into the spanish text shown in the ui
const EVENT_TYPE_LABELS: Record<string, string> = {
  'CABILDO': 'Cabildo',
  'CULTOS': 'Cultos',
  'PROCESION': 'Procesión',
  'ESTACION_PENITENCIA': 'Estación de penitencia',
  'ENSAYO': 'Ensayo',
  'OTRO': 'Otro',
};

const EVENT_STATUS_LABELS: Record<string, string> = {
  'PLANNING': 'Planificación',
  'PREPARATION': 'En preparación',
  'CONFIRMATION': 'Confirmación',
  'CLOSING': 'En cierre',
  'CLOSED': 'Cerrado',
};

const EVENT_STATUS_BADGE_VARIANTS: Record<string, string> = {
  'PLANNING': 'neutral',
  'PREPARATION': 'pending',
  'CONFIRMATION': 'confirmed',
  'CLOSING': 'wood',
  'CLOSED': 'neutral',
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
  'ACCEPTED': 'Confirmada',
  'PENDING': 'Pendiente',
  'REJECTED': 'Rechazada',
};

const DECISION_BADGE_VARIANTS: Record<string, string> = {
  'ACCEPTED': 'confirmed',
  'PENDING': 'pending',
  'REJECTED': 'rejected',
};

const INCIDENT_STATUS_LABELS: Record<string, string> = {
  'OPEN': 'Abierta',
  'RESOLVED': 'Resuelta',
};

const INCIDENT_BADGE_VARIANTS: Record<string, string> = {
  'OPEN': 'pending',
  'RESOLVED': 'confirmed',
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

const STEP_INDEX_MAP: Record<string, number> = {
  'PLANNING': 1,
  'PREPARATION': 2,
  'CONFIRMATION': 3,
  'CLOSING': 4,
  'CLOSED': 4,
};

export function getEventTypeLabel(type: string): string {
  // every getter falls back to the raw code so unknown enum values stay visible instead of disappearing
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
  { value: 'ESTACION_PENITENCIA', label: 'Estación de penitencia' },
  { value: 'ENSAYO', label: 'Ensayo' },
  { value: 'OTRO', label: 'Otro' },
];

export const EVENT_STATUS_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'PLANNING', label: 'Planificación' },
  { value: 'PREPARATION', label: 'En preparación' },
  { value: 'CONFIRMATION', label: 'Confirmación' },
  { value: 'CLOSING', label: 'En cierre' },
];
