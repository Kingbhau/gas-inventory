import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { PageResponse } from '../models/page-response';
import { DayBook, DayBookSummary } from '../models/daybook.model';

@Injectable({
  providedIn: 'root'
})
export class DayBookService {
  private apiUrl = getApiUrl('/daybook');

  constructor(private http: HttpClient) { }

  /**
   * Get all transactions for the current day with pagination
   */
  getCurrentDayTransactions(page: number = 0, pageSize: number = 10, sortBy: string = 'transactionDate', direction: string = 'DESC', createdBy?: string, transactionType?: string): Observable<PageResponse<DayBook>> {
    let params = new HttpParams();
    params = params.set('page', page.toString());
    params = params.set('size', pageSize.toString());
    params = params.set('sortBy', sortBy);
    params = params.set('direction', direction);
    if (createdBy) params = params.set('createdBy', createdBy);
    if (transactionType) params = params.set('transactionType', transactionType);
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<DayBook>>());
  }

  /**
   * Get all transactions for a specific date with pagination
   * @param date - Date in format YYYY-MM-DD
   */
  getTransactionsByDate(date: string, page: number = 0, pageSize: number = 10, sortBy: string = 'transactionDate', direction: string = 'DESC', createdBy?: string, transactionType?: string): Observable<PageResponse<DayBook>> {
    let params = new HttpParams();
    params = params.set('date', date);
    params = params.set('page', page.toString());
    params = params.set('size', pageSize.toString());
    params = params.set('sortBy', sortBy);
    params = params.set('direction', direction);
    if (createdBy) params = params.set('createdBy', createdBy);
    if (transactionType) params = params.set('transactionType', transactionType);
    return this.http.get<any>(`${this.apiUrl}/by-date`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<DayBook>>());
  }

  /**
   * Get summary for a specific date (all transactions, no pagination)
   */
  getTransactionsSummary(date: string, createdBy?: string, transactionType?: string): Observable<DayBookSummary> {
    let params = new HttpParams();
    params = params.set('date', date);
    if (createdBy) params = params.set('createdBy', createdBy);
    if (transactionType) params = params.set('transactionType', transactionType);
    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<DayBookSummary>());
  }
}
