import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentMode } from '../models/payment-mode.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({
  providedIn: 'root'
})
export class PaymentModeService {
  private apiUrl = getApiUrl('/payment-modes');

  constructor(private http: HttpClient) {}

  // Get all payment modes
  getAllPaymentModes(page: number = 0, pageSize: number = 100): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  // Get active payment modes only
  getActivePaymentModes(): Observable<PaymentMode[]> {
    return this.http.get<PaymentMode[]>(`${this.apiUrl}/active`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Get active payment mode names only
  getActivePaymentModeNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/names`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Get payment mode by ID
  getPaymentModeById(id: number): Observable<PaymentMode> {
    return this.http.get<PaymentMode>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Create new payment mode
  createPaymentMode(paymentMode: PaymentMode): Observable<PaymentMode> {
    return this.http.post<PaymentMode>(`${this.apiUrl}`, paymentMode, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Update payment mode
  updatePaymentMode(id: number, paymentMode: PaymentMode): Observable<PaymentMode> {
    return this.http.put<PaymentMode>(`${this.apiUrl}/${id}`, paymentMode, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Delete payment mode
  deletePaymentMode(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Toggle payment mode status
  togglePaymentModeStatus(id: number, isActive: boolean): Observable<PaymentMode> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.http.patch<PaymentMode>(`${this.apiUrl}/${id}/status`, {}, { params, withCredentials: true })
      .pipe(applyTimeout());
  }
}
