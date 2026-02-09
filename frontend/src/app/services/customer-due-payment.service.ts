import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CustomerDuePaymentService {
  private apiUrl = getApiUrl('/customer-due-payment');

  constructor(private http: HttpClient) { }

  getDuePaymentReport(
    page: number = 0,
    size: number = 10,
    sortBy: string = 'dueAmount',
    direction: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    minAmount?: number,
    maxAmount?: number
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    
    return this.http.get<any>(`${this.apiUrl}/report`, { params, withCredentials: true })
      .pipe(timeout(30000));
  }

  getDuePaymentReportSummary(
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    minAmount?: number,
    maxAmount?: number
  ): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    
    return this.http.get<any>(`${this.apiUrl}/report/summary`, { params, withCredentials: true })
      .pipe(timeout(30000));
  }
}
