import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { getApiUrl } from '../config/api.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { DashboardSummary } from '../models/dashboard.model';

export { DashboardSummary };

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = getApiUrl('/dashboard');

  constructor(private http: HttpClient) {}

  /**
   * Get comprehensive dashboard data with optional month selection
   * @param year optional year (defaults to current year)
   * @param month optional month 1-12 (defaults to current month)
   * @param forceRefresh no-op (kept for compatibility)
   */
  getDashboardSummary(year?: number | null, month?: number | null, forceRefresh: boolean = false): Observable<DashboardSummary> {
    let url = `${this.apiUrl}/comprehensive`;
    if (year || month) {
      const params = new URLSearchParams();
      if (year) params.append('year', year.toString());
      if (month) params.append('month', month.toString());
      url += '?' + params.toString();
    }

    return this.http
      .get<any>(url, { withCredentials: true })
      .pipe(
        timeout(30000),
        unwrapApiResponse<DashboardSummary>(),
        catchError(this.handleError)
      );
  }

  /**
   * Force refresh dashboard data (bypass cache)
   */
  refreshDashboard(): Observable<DashboardSummary> {
    return this.getDashboardSummary(null, null, true);
  }

  /**
   * Clear cache
   */
  clearCache(): void {
    // No-op: dashboard caching is disabled
  }

  /**
   * Error handling
   */
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred while loading dashboard data';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }

    return throwError(() => new Error(errorMessage));
  }
}
