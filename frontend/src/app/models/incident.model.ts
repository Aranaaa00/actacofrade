export interface IncidentResponse {
  id: number;
  eventId: number;
  description: string;
  status: string;
  reportedById: number | null;
  reportedByName: string | null;
  reportedByVerified?: boolean;
  resolvedById: number | null;
  resolvedByName: string | null;
  resolvedByVerified?: boolean;
  createdAt: string;
  resolvedAt: string | null;
}

export interface CreateIncidentRequest {
  description: string;
  reportedById?: number | null;
}
