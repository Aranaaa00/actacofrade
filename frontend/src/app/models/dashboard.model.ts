import { EventResponse } from './event.model';

export type DashboardAlertType = 'TASK' | 'INCIDENT' | 'DECISION';

export interface DashboardAlert {
  type: DashboardAlertType;
  description: string;
  eventId: number;
  eventDate: string;
  entityId: number;
}

export interface DashboardData {
  recentEvents: EventResponse[];
  alerts: DashboardAlert[];
  pendingTasksCount: number;
  readyToCloseCount: number;
}
