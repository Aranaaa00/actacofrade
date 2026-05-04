import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogPage } from '../models/audit-log.model';
import { retryReads } from '../shared/utils/retry.utils';

// Retrieves audit history for a given event.
@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/events';

  findByEventId(eventId: number, page: number, size: number): Observable<AuditLogPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AuditLogPage>(`${this.baseUrl}/${eventId}/history`, { params }).pipe(retryReads());
  }
}
