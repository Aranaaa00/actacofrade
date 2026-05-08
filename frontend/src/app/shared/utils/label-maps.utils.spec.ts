import {
  getEventTypeLabel,
  getEventStatusLabel,
  getEventStatusBadgeVariant,
  getTaskStatusLabel,
  getTaskBadgeVariant,
  getDecisionStatusLabel,
  getDecisionBadgeVariant,
  getIncidentStatusLabel,
  getIncidentBadgeVariant,
  getAreaLabel,
  getActionLabel,
  getEntityTypeLabel,
  getHistoryBadgeVariant,
  getStepIndex,
  getSimplifiedTaskStatusLabel,
  getSimplifiedTaskBadgeVariant,
  EVENT_TYPE_OPTIONS,
  EVENT_STATUS_OPTIONS,
} from './label-maps.utils';

describe('label-maps.utils', () => {
  it('translates known event types and falls back to raw code', () => {
    expect(getEventTypeLabel('CABILDO')).toBe('Cabildo');
    expect(getEventTypeLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('translates known event statuses and badges', () => {
    expect(getEventStatusLabel('PLANNING')).toBe('Planificación');
    expect(getEventStatusLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(getEventStatusBadgeVariant('CONFIRMATION')).toBe('confirmed');
    expect(getEventStatusBadgeVariant('UNKNOWN')).toBe('neutral');
  });

  it('translates task statuses and badges', () => {
    expect(getTaskStatusLabel('PLANNED')).toBe('Planificada');
    expect(getTaskStatusLabel('?')).toBe('?');
    expect(getTaskBadgeVariant('COMPLETED')).toBe('confirmed');
    expect(getTaskBadgeVariant('?')).toBe('neutral');
  });

  it('translates decision statuses and badges', () => {
    expect(getDecisionStatusLabel('ACCEPTED')).toBe('Confirmada');
    expect(getDecisionStatusLabel('?')).toBe('?');
    expect(getDecisionBadgeVariant('REJECTED')).toBe('rejected');
    expect(getDecisionBadgeVariant('?')).toBe('neutral');
  });

  it('translates incident statuses and badges', () => {
    expect(getIncidentStatusLabel('OPEN')).toBe('Abierta');
    expect(getIncidentStatusLabel('?')).toBe('?');
    expect(getIncidentBadgeVariant('RESOLVED')).toBe('confirmed');
    expect(getIncidentBadgeVariant('?')).toBe('neutral');
  });

  it('translates area labels with fallback', () => {
    expect(getAreaLabel('MAYORDOMIA')).toBe('Mayordomía');
    expect(getAreaLabel('?')).toBe('?');
  });

  it('translates action labels with fallback', () => {
    expect(getActionLabel('TASK_CREATED')).toBe('Tarea creada');
    expect(getActionLabel('?')).toBe('?');
  });

  it('translates entity type labels and history badges', () => {
    expect(getEntityTypeLabel('TASK')).toBe('Tarea');
    expect(getEntityTypeLabel('?')).toBe('?');
    expect(getHistoryBadgeVariant('EVENT')).toBe('wood');
    expect(getHistoryBadgeVariant('?')).toBe('neutral');
  });

  it('returns step index by status', () => {
    expect(getStepIndex('PLANNING')).toBe(1);
    expect(getStepIndex('CLOSED')).toBe(4);
    expect(getStepIndex('UNKNOWN')).toBe(0);
  });

  it('returns simplified task labels and badges', () => {
    expect(getSimplifiedTaskStatusLabel('ACCEPTED')).toBe('Confirmada');
    expect(getSimplifiedTaskStatusLabel('?')).toBe('?');
    expect(getSimplifiedTaskBadgeVariant('PLANNED')).toBe('pending');
    expect(getSimplifiedTaskBadgeVariant('?')).toBe('neutral');
  });

  it('exposes filter option lists', () => {
    expect(EVENT_TYPE_OPTIONS[0].value).toBe('');
    expect(EVENT_STATUS_OPTIONS.length).toBeGreaterThan(1);
  });
});
