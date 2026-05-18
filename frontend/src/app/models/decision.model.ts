export interface DecisionResponse {
  id: number;
  eventId: number;
  area: string;
  title: string;
  description: string | null;
  deadline: string | null;
  status: string;
  reviewedById: number | null;
  reviewedByName: string | null;
  reviewedByVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDecisionRequest {
  area: string;
  title: string;
  description?: string | null;
  deadline?: string | null;
  reviewedById?: number | null;
}

export interface UpdateDecisionRequest {
  area?: string;
  title?: string;
  description?: string | null;
  deadline?: string | null;
  reviewedById?: number | null;
}
