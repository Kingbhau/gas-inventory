import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { PaymentMode } from '../models/payment-mode.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class PaymentModeService {
  private apiUrl = getApiUrl('/payment-modes');

  constructor(private http: HttpClient) {}

  // Get all payment modes
  getAllPaymentModes(page: number = 0, pageSize: number = 100): Observable<PageResponse<PaymentMode>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<PaymentMode>>());
  }

  getAllPaymentModesAll(pageSize: number = 200): Observable<PaymentMode[]> {
    return this.getAllPaymentModes(0, pageSize).pipe(
      expand((response: PageResponse<PaymentMode>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllPaymentModes(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<PaymentMode>) => response?.items ?? response ?? []),
      reduce((all: PaymentMode[], chunk: PaymentMode[]) => all.concat(chunk), [])
    );
  }

  // Get active payment modes only
  getActivePaymentModes(): Observable<PaymentMode[]> {
    return this.http.get<any>(`${this.apiUrl}/active`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentMode[]>());
  }

  // Get active payment mode names only
  getActivePaymentModeNames(): Observable<string[]> {
    return this.http.get<any>(`${this.apiUrl}/names`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<string[]>());
  }

  // Get payment mode by ID
  getPaymentModeById(id: number): Observable<PaymentMode> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentMode>());
  }

  // Create new payment mode
  createPaymentMode(paymentMode: PaymentMode): Observable<PaymentMode> {
    return this.http.post<any>(`${this.apiUrl}`, paymentMode, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentMode>());
  }

  // Update payment mode
  updatePaymentMode(id: number, paymentMode: PaymentMode): Observable<PaymentMode> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, paymentMode, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentMode>());
  }

  // Delete payment mode
  deletePaymentMode(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  // Toggle payment mode status
  togglePaymentModeStatus(id: number, isActive: boolean): Observable<PaymentMode> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.http.patch<any>(`${this.apiUrl}/${id}/status`, {}, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentMode>());
  }
}
