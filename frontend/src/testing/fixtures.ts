import { AuthResponse } from '../app/models/auth.model';
import { EventResponse } from '../app/models/event.model';
import { TaskResponse, MyTaskResponse } from '../app/models/task.model';
import { DecisionResponse } from '../app/models/decision.model';
import { IncidentResponse } from '../app/models/incident.model';
import { UserResponse } from '../app/models/user.model';

export function buildAuthResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    userId: 1,
    token: 'test-token',
    email: 'user@example.com',
    fullName: 'Test User',
    roles: ['ADMINISTRADOR'],
    hermandadNombre: 'Hermandad Test',
    hasAvatar: false,
    ...overrides,
  };
}

export function buildEvent(overrides: Partial<EventResponse> = {}): EventResponse {
  return {
    id: 1,
    reference: 'E-0001',
    title: 'Cabildo de prueba',
    eventType: 'CABILDO',
    eventDate: '2025-04-10',
    location: 'Sede',
    observations: '',
    status: 'PLANNING',
    responsibleId: 1,
    responsibleName: 'Test User',
    isLockedForClosing: false,
    pendingTasks: 0,
    openIncidents: 0,
    totalTasks: 0,
    completedTasks: 0,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
    ...overrides,
  };
}

export function buildTask(overrides: Partial<TaskResponse> = {}): TaskResponse {
  return {
    id: 1,
    eventId: 1,
    title: 'Tarea',
    description: '',
    assignedToId: 2,
    assignedToName: 'Asignado',
    createdByUserId: 1,
    status: 'PLANNED',
    deadline: null,
    rejectionReason: null,
    confirmedById: null,
    confirmedByName: null,
    confirmedAt: null,
    completedAt: null,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
    ...overrides,
  };
}

export function buildMyTask(overrides: Partial<MyTaskResponse> = {}): MyTaskResponse {
  return {
    id: 1,
    eventId: 1,
    eventType: 'CABILDO',
    eventTitle: 'Cabildo',
    title: 'Tarea',
    status: 'PLANNED',
    deadline: null,
    rejectionReason: null,
    confirmedAt: null,
    completedAt: null,
    updatedAt: '2025-01-01T00:00:00Z',
    ...overrides,
  };
}

export function buildDecision(overrides: Partial<DecisionResponse> = {}): DecisionResponse {
  return {
    id: 1,
    eventId: 1,
    area: 'MAYORDOMIA',
    title: 'Decisión',
    description: null,
    deadline: null,
    status: 'PENDING',
    reviewedById: null,
    reviewedByName: null,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
    ...overrides,
  };
}

export function buildIncident(overrides: Partial<IncidentResponse> = {}): IncidentResponse {
  return {
    id: 1,
    eventId: 1,
    description: 'Incidencia',
    notes: null,
    deadline: null,
    status: 'OPEN',
    reportedById: 1,
    reportedByName: 'Test User',
    resolvedById: null,
    resolvedByName: null,
    createdAt: '2025-01-01T00:00:00Z',
    resolvedAt: null,
    ...overrides,
  };
}

export function buildUser(overrides: Partial<UserResponse> = {}): UserResponse {
  return {
    id: 1,
    fullName: 'Test User',
    email: 'user@example.com',
    roles: ['ADMINISTRADOR'],
    active: true,
    lastLogin: null,
    hasAvatar: false,
    ...overrides,
  };
}
