
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupplierTransaction } from '../models/supplier-transaction.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({
  providedIn: 'root'
})
export class SupplierTransactionService {
  private apiUrl = getApiUrl('/supplier-transactions');

  constructor(private http: HttpClient) { }

  deleteTransaction(id: number | string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }
    updateTransaction(id: number | string, transaction: any): Observable<SupplierTransaction> {
    return this.http.put<SupplierTransaction>(`${this.apiUrl}/${id}`, transaction, { withCredentials: true })
      .pipe(applyTimeout());
  }

  recordTransaction(transaction: any): Observable<SupplierTransaction> {
    return this.http.post<SupplierTransaction>(this.apiUrl, transaction, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getTransaction(id: number): Observable<SupplierTransaction> {
    return this.http.get<SupplierTransaction>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllTransactions(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getTransactionsBySupplier(supplierId: number): Observable<SupplierTransaction[]> {
    return this.http.get<SupplierTransaction[]>(`${this.apiUrl}/supplier/${supplierId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
