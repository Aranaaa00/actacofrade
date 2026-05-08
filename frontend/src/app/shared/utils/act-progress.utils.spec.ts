import {
  calculateActProgress,
  getProgressMessage,
  getPendingActionsText,
  buildProgressSteps,
  isTaskPending,
  isDecisionPending,
  isIncidentPending,
} from './act-progress.utils';
import { buildTask, buildDecision, buildIncident } from '../../../testing/fixtures';

describe('act-progress.utils', () => {
  describe('calculateActProgress', () => {
    it('returns zero progress when no items exist', () => {
      expect(calculateActProgress([], [], [])).toEqual({ total: 0, pending: 0, percent: 0 });
    });

    it('computes progress combining tasks, decisions and incidents', () => {
      const tasks = [
        buildTask({ status: 'COMPLETED' }),
        buildTask({ status: 'PLANNED' }),
      ];
      const decisions = [buildDecision({ status: 'ACCEPTED' })];
      const incidents = [buildIncident({ status: 'OPEN' })];
      const progress = calculateActProgress(tasks, decisions, incidents);
      expect(progress.total).toBe(4);
      expect(progress.pending).toBe(2);
      expect(progress.percent).toBeCloseTo(50, 0);
    });

    it('treats unknown statuses as zero weight', () => {
      const tasks = [buildTask({ status: 'WHATEVER' })];
      const progress = calculateActProgress(tasks, [], []);
      expect(progress.percent).toBe(0);
      expect(progress.pending).toBe(1);
    });
  });

  describe('isTaskPending / isDecisionPending / isIncidentPending', () => {
    it('correctly identifies pending state', () => {
      expect(isTaskPending('PLANNED')).toBe(true);
      expect(isTaskPending('COMPLETED')).toBe(false);
      expect(isDecisionPending('PENDING')).toBe(true);
      expect(isDecisionPending('ACCEPTED')).toBe(false);
      expect(isIncidentPending('OPEN')).toBe(true);
      expect(isIncidentPending('RESOLVED')).toBe(false);
    });

    it('treats unknown status as pending', () => {
      expect(isTaskPending('?')).toBe(true);
      expect(isDecisionPending('?')).toBe(true);
      expect(isIncidentPending('?')).toBe(true);
    });
  });

  describe('getProgressMessage', () => {
    it('returns specific message for empty acts', () => {
      expect(getProgressMessage(0, 0)).toContain('Sin acciones');
    });

    it('returns ready message at 100%', () => {
      expect(getProgressMessage(100, 5)).toContain('cerrar');
    });

    it('returns near-ready message at 80%', () => {
      expect(getProgressMessage(80, 5)).toContain('Casi');
    });

    it('returns advancing message at 50%', () => {
      expect(getProgressMessage(50, 5)).toContain('avanzando');
    });

    it('returns starting message at 10%', () => {
      expect(getProgressMessage(10, 5)).toContain('empezando');
    });

    it('returns pending work message at 0%', () => {
      expect(getProgressMessage(0, 5)).toContain('queda trabajo');
    });
  });

  describe('getPendingActionsText', () => {
    it('returns no pending message when zero', () => {
      expect(getPendingActionsText(0)).toContain('Sin acciones');
    });

    it('returns count when pending', () => {
      expect(getPendingActionsText(3)).toContain('3');
    });
  });

  describe('buildProgressSteps', () => {
    it('returns four steps with proper completion flags', () => {
      const steps = buildProgressSteps(50);
      expect(steps.length).toBe(4);
      expect(steps[0].done).toBe(true);
      expect(steps[1].done).toBe(true);
      expect(steps[2].done).toBe(false);
      expect(steps[3].done).toBe(false);
    });

    it('clamps connector fill between 0 and 100', () => {
      const stepsAtZero = buildProgressSteps(0);
      expect(stepsAtZero[0].connectorFill).toBe(0);
      const stepsAtFull = buildProgressSteps(100);
      expect(stepsAtFull[0].connectorFill).toBe(100);
    });

    it('marks connectors done when next threshold is reached', () => {
      const steps = buildProgressSteps(70);
      expect(steps[0].connectorDone).toBe(true);
      expect(steps[1].connectorDone).toBe(true);
      expect(steps[2].connectorDone).toBe(false);
    });
  });
});
