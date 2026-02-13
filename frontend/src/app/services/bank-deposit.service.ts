import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { BankDeposit } from '../models/bank-deposit.model';
import { BankDepositSummary } from '../models/bank-deposit-summary.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class BankDepositService {
  private apiUrl = getApiUrl('/bank-deposits');

  constructor(private http: HttpClient) { }

  /**
   * Create a new bank deposit record
   */
  createDeposit(deposit: BankDeposit): Observable<BankDeposit> {
    return this.http.post<any>(this.apiUrl, deposit, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<BankDeposit>());
  }

  /**
   * Get deposit by ID
   */
  getDepositById(id: string): Observable<BankDeposit> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<BankDeposit>());
  }

  /**
   * Update an existing bank deposit
   */
  updateDeposit(id: string, deposit: BankDeposit): Observable<BankDeposit> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, deposit, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<BankDeposit>());
  }

  /**
   * Delete a bank deposit
   */
  deleteDeposit(id: string): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<SimpleStatusDTO>());
  }

  /**
   * Get all bank deposits with pagination and filtering
   */
  getDeposits(
    page: number = 0,
    size: number = 10,
    sortBy: string = 'depositDate',
    sortOrder: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    bankAccountId?: number,
    paymentMode?: string,
    referenceNumber?: string,
    createdBy?: string
  ): Observable<PageResponse<BankDeposit>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortOrder', sortOrder);

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId.toString());
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);
    if (createdBy) params = params.set('createdBy', createdBy);

    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<PageResponse<BankDeposit>>());
  }

  /**
   * Get deposit summary by date range and filters
   */
  getDepositSummary(
    fromDate: string,
    toDate: string,
    bankAccountId?: number,
    paymentMode?: string,
    referenceNumber?: string,
    createdBy?: string
  ): Observable<BankDepositSummary> {
    let params = new HttpParams();
    
    if (fromDate && fromDate.trim()) {
      params = params.set('fromDate', fromDate);
    }
    if (toDate && toDate.trim()) {
      params = params.set('toDate', toDate);
    }
    if (bankAccountId) {
      params = params.set('bankAccountId', bankAccountId.toString());
    }
    if (paymentMode && paymentMode.trim()) {
      params = params.set('paymentMode', paymentMode);
    }
    if (referenceNumber && referenceNumber.trim()) {
      params = params.set('referenceNumber', referenceNumber);
    }
    if (createdBy && createdBy.trim()) {
      params = params.set('createdBy', createdBy);
    }

    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<BankDepositSummary>());
  }
}
