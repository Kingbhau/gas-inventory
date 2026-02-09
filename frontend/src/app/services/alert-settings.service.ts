import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface AlertConfig {
  id: number;
  alertType: string;
  enabled: boolean;
  filledCylinderThreshold?: number;
  emptyCylinderThreshold?: number;
  pendingReturnThreshold?: number;
  description: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AlertSettingsService {
  private apiUrl = getApiUrl('/alerts/config');

  constructor(private http: HttpClient) { }

  /**
   * Get all alert configurations
   */
  getAlertConfigurations(): Observable<ApiResponse<AlertConfig[]>> {
    return this.http.get<ApiResponse<AlertConfig[]>>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Get a specific alert configuration by type
   */
  getAlertConfig(alertType: string): Observable<ApiResponse<AlertConfig>> {
    return this.http.get<ApiResponse<AlertConfig>>(`${this.apiUrl}/${alertType}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Update alert configuration
   */
  updateAlertConfig(alertType: string, config: any): Observable<ApiResponse<AlertConfig>> {
    return this.http.put<ApiResponse<AlertConfig>>(`${this.apiUrl}/${alertType}`, config, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Enable or disable an alert type
   */
  toggleAlertConfig(alertType: string, enabled: boolean): Observable<ApiResponse<AlertConfig>> {
    return this.http.patch<ApiResponse<AlertConfig>>(
      `${this.apiUrl}/${alertType}/toggle`,
      { enabled },
      { withCredentials: true }
    ).pipe(applyTimeout());
  }
}
