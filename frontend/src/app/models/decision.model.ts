export interface DecisionResponse {
  id: number;
  eventId: number;
  area: string;
  title: string;
  status: string;
  reviewedById: number | null;
  reviewedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDecisionRequest {
  area: string;
  title: string;
  reviewedById?: number | null;
}

export interface UpdateDecisionRequest {
  area?: string;
  title?: string;
  reviewedById?: number | null;
}
