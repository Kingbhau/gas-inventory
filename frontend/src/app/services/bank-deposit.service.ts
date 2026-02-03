import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { BankDeposit } from '../models/index';
import { getApiUrl } from '../config/api.config';

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
    return this.http.post<BankDeposit>(this.apiUrl, deposit, { withCredentials: true })
      .pipe(timeout(30000));
  }

  /**
   * Get deposit by ID
   */
  getDepositById(id: string): Observable<BankDeposit> {
    return this.http.get<BankDeposit>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000));
  }

  /**
   * Update an existing bank deposit
   */
  updateDeposit(id: string, deposit: BankDeposit): Observable<BankDeposit> {
    return this.http.put<BankDeposit>(`${this.apiUrl}/${id}`, deposit, { withCredentials: true })
      .pipe(timeout(30000));
  }

  /**
   * Delete a bank deposit
   */
  deleteDeposit(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000));
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
    referenceNumber?: string
  ): Observable<any> {
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

    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(timeout(30000));
  }

  /**
   * Get deposit summary by date range and filters
   */
  getDepositSummary(
    fromDate: string,
    toDate: string,
    bankAccountId?: number,
    paymentMode?: string,
    referenceNumber?: string
  ): Observable<any> {
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

    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(timeout(30000));
  }
}
