export interface TaskResponse {
  id: number;
  eventId: number;
  title: string;
  description: string;
  assignedToId: number | null;
  assignedToName: string | null;
  createdByUserId: number | null;
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

export interface MyTaskResponse {
  id: number;
  eventId: number;
  eventType: string;
  eventTitle: string;
  title: string;
  status: string;
  deadline: string | null;
  rejectionReason: string | null;
  confirmedAt: string | null;
  completedAt: string | null;
  updatedAt: string;
}

export interface MyTaskPage {
  content: MyTaskResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface MyTaskStats {
  pendingCount: number;
  confirmedCount: number;
  rejectedCount: number;
}
