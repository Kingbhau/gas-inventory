
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupplierTransaction } from '../models/supplier-transaction.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { CreateSupplierTransactionRequest } from '../models/create-supplier-transaction-request.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class SupplierTransactionService {
  private apiUrl = getApiUrl('/supplier-transactions');

  constructor(private http: HttpClient) { }

  deleteTransaction(id: number | string): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }
  updateTransaction(id: number | string, transaction: CreateSupplierTransactionRequest): Observable<SupplierTransaction> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, transaction, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SupplierTransaction>());
  }

  recordTransaction(transaction: CreateSupplierTransactionRequest): Observable<SupplierTransaction> {
    return this.http.post<any>(this.apiUrl, transaction, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SupplierTransaction>());
  }

  getTransaction(id: number): Observable<SupplierTransaction> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SupplierTransaction>());
  }

  getAllTransactions(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC', referenceNumber?: string, createdBy?: string): Observable<PageResponse<SupplierTransaction>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    if (referenceNumber) {
      params = params.set('referenceNumber', referenceNumber);
    }
    if (createdBy) {
      params = params.set('createdBy', createdBy);
    }
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<SupplierTransaction>>());
  }

  getTransactionsBySupplier(supplierId: number): Observable<SupplierTransaction[]> {
    return this.http.get<any>(`${this.apiUrl}/supplier/${supplierId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SupplierTransaction[]>());
  }

  getTransactionsByWarehouse(warehouseId: number): Observable<SupplierTransaction[]> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SupplierTransaction[]>());
  }
}
