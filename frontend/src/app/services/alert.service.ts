import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface Alert {
  id: number;
  alertType: string;
  alertKey: string;
  warehouseId?: number;
  customerId?: number;
  message: string;
  severity: 'warning' | 'critical';
  isDismissed: boolean;
  createdAt: string;
  expiresAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

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
  
  constructor(private http: HttpClient) {
    // Don't initialize automatically - only after login
    console.log('AlertService instantiated');
  }
  
  /**
   * Initialize alerts - called after successful login
   */
  public initialize(): void {
    if (!this.initialized) {
      try {
        this.initializeAlerts();
        this.initialized = true;
      } catch (error) {
        console.error('Failed to initialize alert service:', error);
      }
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
    this.initSSE();
  }
  
  /**
   * Load initial alerts from server
   */
  private loadInitialAlerts(): void {
    console.log('📡 Loading initial alerts from:', this.apiUrl);
    this.http.get<ApiResponse<any>>(`${this.apiUrl}`, { withCredentials: true })
      .pipe(applyTimeout())
      .subscribe(
        (response) => {
          console.log('✅ Initial alerts response:', response);
          if (response && response.success && response.data) {
            const alerts = response.data.alerts || [];
            const count = response.data.count || 0;
            console.log(`✅ Loaded ${count} alerts`);
            this.alerts$.next(alerts);
            this.alertCount$.next(count);
          } else {
            console.log('No alerts found in response, setting empty');
            this.alerts$.next([]);
            this.alertCount$.next(0);
          }
        },
        (error) => {
          console.warn('⚠️ Error loading initial alerts:', error?.message || error);
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
      const sseUrl = `${this.apiUrl}/stream`;
      console.log('🔌 Initiating SSE connection to:', sseUrl);
      
      // Create EventSource with credentials
      this.eventSource = new EventSource(sseUrl, { withCredentials: true });
      
      // Handle connection open
      this.eventSource.onopen = () => {
        console.log('✅ SSE connection established');
        this.reconnectAttempts = 0; // Reset reconnect counter on successful connection
      };

      // Handle connection confirmation
      this.eventSource.addEventListener('connected', (event: any) => {
        console.log('✅ SSE connection confirmed:', event.data);
      });

      // Handle keep-alive messages
      this.eventSource.addEventListener('keep-alive', (event: any) => {
        console.log('💓 SSE keep-alive received - connection active');
      });
      
      // Handle new alerts
      this.eventSource.addEventListener('alert', (event: any) => {
        try {
          console.log('📨 Alert received from SSE:', event.data);
          const alert: Alert = JSON.parse(event.data);
          this.addAlert(alert);
        } catch (e) {
          console.error('Error parsing alert data:', e);
        }
      });
      
      // Handle alert dismissals
      this.eventSource.addEventListener('alert-dismissed', (event: any) => {
        try {
          console.log('🗑️ Alert dismissal received:', event.data);
          const alertId = parseInt(event.data);
          this.removeAlert(alertId);
        } catch (e) {
          console.error('Error parsing dismissal data:', e);
        }
      });
      
      this.eventSource.onerror = (error) => {
        console.error('❌ SSE connection error:', error);
        console.error('SSE readyState:', this.eventSource?.readyState);
        
        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }
        
        // Attempt reconnect with limit - but only for connection errors, not 404s
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 15000); // Exponential backoff
          console.log(`🔄 SSE reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms...`);
          setTimeout(() => {
            this.initSSE();
          }, delay);
        } else {
          console.warn('⚠️ Max SSE reconnection attempts reached. Alerts may be unavailable.');
        }
      };
    } catch (error) {
      console.error('Failed to initialize SSE:', error);
    }
  }
  
  /**
   * Add alert to local state and update count
   */
  private addAlert(alert: Alert): void {
    console.log('Adding alert:', alert);
    const current = this.alerts$.value;
    
    // Check if alert already exists
    const exists = current.some(a => a.alertKey === alert.alertKey);
    if (!exists) {
      current.push(alert);
      this.alerts$.next([...current]);
      this.alertCount$.next(current.length);
      console.log('✅ Alert added. Total alerts:', current.length, 'Message:', alert.message);
    } else {
      console.log('⚠️ Alert already exists, skipping duplicate:', alert.alertKey);
    }
  }
  
  /**
   * Remove alert from local state
   */
  private removeAlert(alertId: number): void {
    console.log('Removing alert:', alertId);
    const current = this.alerts$.value;
    const filtered = current.filter(a => a.id !== alertId);
    this.alerts$.next(filtered);
    this.alertCount$.next(filtered.length);
    console.log('✅ Alert removed. Total alerts remaining:', filtered.length);
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
  }
}
