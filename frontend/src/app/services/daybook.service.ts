import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class DayBookService {
  private apiUrl = getApiUrl('/daybook');

  constructor(private http: HttpClient) { }

  /**
   * Get all transactions for the current day with pagination
   */
  getCurrentDayTransactions(page: number = 0, pageSize: number = 10, sortBy: string = 'transactionDate', direction: string = 'DESC'): Observable<any> {
    let params = new HttpParams();
    params = params.set('page', page.toString());
    params = params.set('size', pageSize.toString());
    params = params.set('sortBy', sortBy);
    params = params.set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}`, { params });
  }

  /**
   * Get all transactions for a specific date with pagination
   * @param date - Date in format YYYY-MM-DD
   */
  getTransactionsByDate(date: string, page: number = 0, pageSize: number = 10, sortBy: string = 'transactionDate', direction: string = 'DESC'): Observable<any> {
    let params = new HttpParams();
    params = params.set('date', date);
    params = params.set('page', page.toString());
    params = params.set('size', pageSize.toString());
    params = params.set('sortBy', sortBy);
    params = params.set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/by-date`, { params });
  }

  /**
   * Get summary for a specific date (all transactions, no pagination)
   */
  getTransactionsSummary(date: string): Observable<any> {
    let params = new HttpParams();
    params = params.set('date', date);
    return this.http.get<any>(`${this.apiUrl}/summary`, { params });
  }
}
