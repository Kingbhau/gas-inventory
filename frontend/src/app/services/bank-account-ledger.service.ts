import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
}

export interface BankAccountLedgerDTO {
  id: number;
  bankAccountId: number;
  bankAccountName: string;
  transactionType: string;
  amount: number;
  saleId: number;
  referenceNumber: string;
  description: string;
  transactionDate: string;
}

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
  ): Observable<PageResponse<BankAccountLedgerDTO>> {
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

    return this.http.get<PageResponse<BankAccountLedgerDTO>>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getBankTransactionById(id: number): Observable<BankAccountLedgerDTO> {
    return this.http.get<BankAccountLedgerDTO>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getBankTransactionsSummary(
    fromDate?: string,
    toDate?: string,
    bankAccountId?: string,
    transactionType?: string,
    referenceNumber?: string
  ): Observable<any> {
    let params = new HttpParams();

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId);
    if (transactionType) params = params.set('transactionType', transactionType);
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);

    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }
}
