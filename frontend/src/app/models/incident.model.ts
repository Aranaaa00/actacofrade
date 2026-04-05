export interface IncidentResponse {
  id: number;
  eventId: number;
  description: string;
  status: string;
  reportedById: number | null;
  reportedByName: string | null;
  resolvedById: number | null;
  resolvedByName: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export interface CreateIncidentRequest {
  description: string;
  reportedById?: number | null;
}
