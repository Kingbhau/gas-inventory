import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { PageResponse } from '../models/page-response';
import { BankAccountLedger, BankAccountLedgerSummary } from '../models/bank-account-ledger.model';

@Injectable({
  providedIn: 'root'
})
export class BankAccountLedgerService {
  private apiUrl = getApiUrl('/bank-account-ledgers');

  constructor(private http: HttpClient) { }

  getAllBankTransactions(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'transactionDate',
    direction: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    bankAccountId?: string,
    transactionType?: string,
    referenceNumber?: string
  ): Observable<PageResponse<BankAccountLedger>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId);
    if (transactionType) params = params.set('transactionType', transactionType);
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);

    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<BankAccountLedger>>());
  }

  getBankTransactionById(id: number): Observable<BankAccountLedger> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccountLedger>());
  }

  getBankTransactionsSummary(
    fromDate?: string,
    toDate?: string,
    bankAccountId?: string,
    transactionType?: string,
    referenceNumber?: string
  ): Observable<BankAccountLedgerSummary> {
    let params = new HttpParams();

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId);
    if (transactionType) params = params.set('transactionType', transactionType);
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);

    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccountLedgerSummary>());
  }
}
