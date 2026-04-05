export interface EventResponse {
  id: number;
  reference: string;
  title: string;
  eventType: string;
  eventDate: string;
  location: string;
  observations: string;
  status: string;
  responsibleId: number | null;
  responsibleName: string | null;
  isLockedForClosing: boolean;
  pendingTasks: number;
  openIncidents: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEventRequest {
  title: string;
  eventType: string;
  eventDate: string;
  location?: string;
  responsibleId?: number | null;
  observations?: string;
}

export interface UpdateEventRequest {
  title?: string;
  eventType?: string;
  eventDate?: string;
  location?: string;
  responsibleId?: number | null;
  observations?: string;
}
