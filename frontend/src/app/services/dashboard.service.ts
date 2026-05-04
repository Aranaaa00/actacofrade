import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardData } from '../models/dashboard.model';
import { retryReads } from '../shared/utils/retry.utils';

// Provides aggregated KPIs for the home dashboard.
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/dashboard';

  getDashboard(): Observable<DashboardData> {
    return this.http.get<DashboardData>(this.baseUrl).pipe(retryReads());
  }
}
