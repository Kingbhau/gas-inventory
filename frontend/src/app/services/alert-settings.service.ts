import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { AlertConfig } from '../models/alert-config.model';

@Injectable({
  providedIn: 'root'
})
export class AlertSettingsService {
  private apiUrl = getApiUrl('/alerts/config');

  constructor(private http: HttpClient) { }

  /**
   * Get all alert configurations
   */
  getAlertConfigurations(): Observable<AlertConfig[]> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<AlertConfig[]>());
  }

  /**
   * Get a specific alert configuration by type
   */
  getAlertConfig(alertType: string): Observable<AlertConfig> {
    return this.http.get<any>(`${this.apiUrl}/${alertType}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<AlertConfig>());
  }

  /**
   * Update alert configuration
   */
  updateAlertConfig(alertType: string, config: Partial<AlertConfig>): Observable<AlertConfig> {
    return this.http.put<any>(`${this.apiUrl}/${alertType}`, config, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<AlertConfig>());
  }

  /**
   * Enable or disable an alert type
   */
  toggleAlertConfig(alertType: string, enabled: boolean): Observable<AlertConfig> {
    return this.http.patch<any>(
      `${this.apiUrl}/${alertType}/toggle`,
      { enabled },
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<AlertConfig>());
  }
}
