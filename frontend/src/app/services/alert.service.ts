import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, timer } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { ApiResponse } from '../models/api-response';
import { Alert } from '../models/alert.model';
import { AlertSummary } from '../models/alert-summary.model';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = getApiUrl('/alerts');
  
  private alerts$ = new BehaviorSubject<Alert[]>([]);
  private alertCount$ = new BehaviorSubject<number>(0);
  private eventSource: EventSource | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;
  private initialized = false;
  private pollSubscription: Subscription | null = null;
  private pollIntervalMs = 30000;
  
  constructor(private http: HttpClient) {
    // Don't initialize automatically - only after login
  }
  
  /**
   * Initialize alerts - called after successful login or on page refresh if still authenticated
   * This will reinitialize even if previously initialized to handle page refresh scenarios
   */
  public initialize(): void {
    try {
      // Close any existing SSE connection and clear alerts before reinitializing
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
      this.stopPolling();
      // Don't clear alerts here - we want to preserve them if already loaded
      // Only clear if this is the first initialization
      if (!this.initialized) {
        this.alerts$.next([]);
        this.alertCount$.next(0);
      }
      this.initializeAlerts();
      this.initialized = true;
    } catch (error) {
    }
  }
  
  /**
   * Reset alerts on logout
   */
  public reset(): void {
    this.alerts$.next([]);
    this.alertCount$.next(0);
    this.initialized = false;
    this.reconnectAttempts = 0;
    this.stopPolling();
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
  
  /**
   * Initialize alerts: load initial state and setup SSE connection
   */
  private initializeAlerts(): void {
    this.loadInitialAlerts();
    // Start polling immediately as a fallback; SSE will disable it on connect.
    this.startPolling();
    this.initSSE();
  }
  
  /**
   * Load initial alerts from server
   */
  private loadInitialAlerts(): void {
    this.http.get<ApiResponse<AlertSummary>>(`${this.apiUrl}`, { withCredentials: true })
      .pipe(applyTimeout())
      .subscribe(
        (response) => {
          if (response && response.data) {
            const alerts = response.data.alerts || [];
            const count = response.data.count || 0;
            this.alerts$.next(alerts);
            this.alertCount$.next(count);
          } else {
            this.alerts$.next([]);
            this.alertCount$.next(0);
          }
        },
        (error) => {
          // Don't fail - just proceed with empty alerts
          this.alerts$.next([]);
          this.alertCount$.next(0);
        }
      );
  }
  
  /**
   * Initialize SSE (Server-Sent Events) for real-time alerts
   * Establishes persistent connection to receive alerts as they happen
   */
  private initSSE(): void {
    try {
      if (typeof EventSource === 'undefined') {
        this.startPolling();
        return;
      }

      const sseUrl = `${this.apiUrl}/stream`;
      
      // Create EventSource with credentials
      this.eventSource = new EventSource(sseUrl, { withCredentials: true });
      
      // Handle connection open
      this.eventSource.onopen = () => {
        this.reconnectAttempts = 0; // Reset reconnect counter on successful connection
        this.stopPolling();
      };

      // Handle connection confirmation
      this.eventSource.addEventListener('connected', (event: any) => {
      });

      // Handle keep-alive messages
      this.eventSource.addEventListener('keep-alive', (event: any) => {
      });
      
      // Handle new alerts
      this.eventSource.addEventListener('alert', (event: any) => {
        try {
          const alert: Alert = JSON.parse(event.data);
          this.addAlert(alert);
        } catch (e) {
        }
      });
      
      // Handle alert dismissals
      this.eventSource.addEventListener('alert-dismissed', (event: any) => {
        try {
          const alertId = parseInt(event.data);
          this.removeAlert(alertId);
        } catch (e) {
        }
      });
      
      this.eventSource.onerror = (error) => {

        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }
        
        // Attempt reconnect with limit - but only for connection errors, not 404s
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 15000); // Exponential backoff
          setTimeout(() => {
            this.initSSE();
          }, delay);
        } else {
          this.startPolling();
        }
      };
    } catch (error) {
      this.startPolling();
    }
  }

  /**
   * Poll alerts periodically as a fallback when SSE is unavailable.
   */
  private startPolling(): void {
    if (this.pollSubscription) {
      return;
    }
    this.pollSubscription = timer(0, this.pollIntervalMs).subscribe(() => {
      this.loadInitialAlerts();
    });
  }

  private stopPolling(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
      this.pollSubscription = null;
    }
  }
  
  /**
   * Add alert to local state and update count
   * Uses both id and alertKey to prevent duplicates
   */
  private addAlert(alert: Alert): void {
    const current = this.alerts$.value;
    
    // Check if alert already exists by either id or alertKey
    const exists = current.some(a => a.id === alert.id || a.alertKey === alert.alertKey);
    if (!exists) {
      current.push(alert);
      this.alerts$.next([...current]);
      this.alertCount$.next(current.length);
    } else {
    }
  }
  
  /**
   * Remove alert from local state
   */
  private removeAlert(alertId: number): void {
    const current = this.alerts$.value;
    const filtered = current.filter(a => a.id !== alertId);
    this.alerts$.next(filtered);
    this.alertCount$.next(filtered.length);
  }
  
  /**
   * Dismiss alert and notify server
   */
  dismissAlert(alertId: number): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/${alertId}/dismiss`,
      {},
      { withCredentials: true }
    ).pipe(applyTimeout());
  }
  
  /**
   * Get all active alerts as Observable
   */
  getAlerts$(): Observable<Alert[]> {
    return this.alerts$.asObservable();
  }
  
  /**
   * Get alert count as Observable
   */
  getAlertCount$(): Observable<number> {
    return this.alertCount$.asObservable();
  }
  
  /**
   * Cleanup SSE connection
   */
  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
    this.stopPolling();
  }
}
