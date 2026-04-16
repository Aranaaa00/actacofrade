export interface TaskResponse {
  id: number;
  eventId: number;
  title: string;
  description: string;
  assignedToId: number | null;
  assignedToName: string | null;
  status: string;
  deadline: string | null;
  rejectionReason: string | null;
  confirmedById: number | null;
  confirmedByName: string | null;
  confirmedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  assignedToId?: number | null;
  deadline?: string | null;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  assignedToId?: number | null;
  deadline?: string | null;
}
