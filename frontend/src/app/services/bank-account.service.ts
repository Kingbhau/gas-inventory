import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BankAccount } from '../models/bank-account.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
}

@Injectable({
  providedIn: 'root'
})
export class BankAccountService {
  private apiUrl = getApiUrl('/bank-accounts');

  constructor(private http: HttpClient) { }

  createBankAccount(bankAccount: BankAccount): Observable<BankAccount> {
    return this.http.post<BankAccount>(this.apiUrl, bankAccount, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getBankAccount(id: number): Observable<BankAccount> {
    return this.http.get<BankAccount>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllBankAccounts(page: number = 0, size: number = 20, sortBy: string = 'createdAt', direction: string = 'DESC'): Observable<PageResponse<BankAccount>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<PageResponse<BankAccount>>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getActiveBankAccounts(): Observable<BankAccount[]> {
    return this.http.get<BankAccount[]>(`${this.apiUrl}/active/list`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  updateBankAccount(id: number, bankAccount: BankAccount): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.apiUrl}/${id}`, bankAccount, { withCredentials: true })
      .pipe(applyTimeout());
  }

  deleteBankAccount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  deactivateBankAccount(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/deactivate`, null, { withCredentials: true })
      .pipe(applyTimeout());
  }

  activateBankAccount(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/activate`, null, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
