import { TaskResponse } from '../../models/task.model';
import { DecisionResponse } from '../../models/decision.model';
import { IncidentResponse } from '../../models/incident.model';

const TASK_PROGRESS_WEIGHTS: Record<string, number> = {
  'PLANNED': 0,
  'ACCEPTED': 0.25,
  'IN_PREPARATION': 0.5,
  'CONFIRMED': 0.75,
  'COMPLETED': 1,
  'REJECTED': 1,
};

const DECISION_PROGRESS_WEIGHTS: Record<string, number> = {
  'PENDING': 0,
  'ACCEPTED': 1,
  'REJECTED': 1,
};

const INCIDENT_PROGRESS_WEIGHTS: Record<string, number> = {
  'ABIERTA': 0,
  'RESUELTA': 1,
};

export interface ActProgress {
  total: number;
  pending: number;
  percent: number;
}

function isTaskPending(status: string): boolean {
  return (TASK_PROGRESS_WEIGHTS[status] ?? 0) < 1;
}

function isDecisionPending(status: string): boolean {
  return (DECISION_PROGRESS_WEIGHTS[status] ?? 0) < 1;
}

function isIncidentPending(status: string): boolean {
  return (INCIDENT_PROGRESS_WEIGHTS[status] ?? 0) < 1;
}

// Computes weighted progress for an act based on tasks, decisions and incidents.
export function calculateActProgress(
  tasks: TaskResponse[],
  decisions: DecisionResponse[],
  incidents: IncidentResponse[]
): ActProgress {
  const total = tasks.length + decisions.length + incidents.length;
  if (total === 0) {
    return { total: 0, pending: 0, percent: 0 };
  }
  const sum =
    tasks.reduce((acc, t) => acc + (TASK_PROGRESS_WEIGHTS[t.status] ?? 0), 0) +
    decisions.reduce((acc, d) => acc + (DECISION_PROGRESS_WEIGHTS[d.status] ?? 0), 0) +
    incidents.reduce((acc, i) => acc + (INCIDENT_PROGRESS_WEIGHTS[i.status] ?? 0), 0);
  const pending =
    tasks.filter(t => isTaskPending(t.status)).length +
    decisions.filter(d => isDecisionPending(d.status)).length +
    incidents.filter(i => isIncidentPending(i.status)).length;
  const percent = (sum / total) * 100;
  return { total, pending, percent };
}

// Returns a friendly progress message tailored to the completion percentage.
export function getProgressMessage(percent: number, total: number): string {
  if (total === 0) {
    return 'Sin acciones registradas';
  }
  if (percent >= 100) {
    return 'Todo listo para cerrar';
  }
  if (percent >= 75) {
    return 'Casi todo está listo';
  }
  if (percent >= 40) {
    return 'El acto está avanzando';
  }
  if (percent > 0) {
    return 'El acto está empezando';
  }
  return 'Aún queda trabajo por hacer';
}

export function getPendingActionsText(pending: number): string {
  if (pending <= 0) {
    return 'Sin acciones pendientes';
  }
  if (pending === 1) {
    return 'Queda 1 acción';
  }
  return `Quedan ${pending} acciones`;
}

export interface ActProgressStep {
  key: string;
  label: string;
  threshold: number;
  done: boolean;
  connectorDone: boolean;
  connectorFill: number;
}

const PROGRESS_STEPS: ReadonlyArray<{ key: string; label: string; threshold: number }> = [
  { key: 'INICIO', label: 'Sin empezar', threshold: 0 },
  { key: 'EN_MARCHA', label: 'En marcha', threshold: 33 },
  { key: 'CASI_LISTO', label: 'Casi listo', threshold: 66 },
  { key: 'LISTO', label: 'Listo', threshold: 100 },
];

// Builds the four-step model used by the visual progress bar.
export function buildProgressSteps(percent: number): ActProgressStep[] {
  return PROGRESS_STEPS.map((s, i) => {
    const next = PROGRESS_STEPS[i + 1];
    let connectorFill = 0;
    if (next) {
      const span = next.threshold - s.threshold;
      const raw = span > 0 ? ((percent - s.threshold) / span) * 100 : 0;
      connectorFill = Math.max(0, Math.min(100, raw));
    }
    return {
      key: s.key,
      label: s.label,
      threshold: s.threshold,
      done: percent >= s.threshold,
      connectorDone: !!next && percent >= next.threshold,
      connectorFill,
    };
  });
}
