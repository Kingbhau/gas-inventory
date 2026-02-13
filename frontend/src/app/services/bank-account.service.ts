import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { BankAccount } from '../models/bank-account.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class BankAccountService {
  private apiUrl = getApiUrl('/bank-accounts');

  constructor(private http: HttpClient) { }

  createBankAccount(bankAccount: BankAccount): Observable<BankAccount> {
    return this.http.post<any>(this.apiUrl, bankAccount, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccount>());
  }

  getBankAccount(id: number): Observable<BankAccount> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccount>());
  }

  getAllBankAccounts(page: number = 0, size: number = 20, sortBy: string = 'createdAt', direction: string = 'DESC'): Observable<PageResponse<BankAccount>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<BankAccount>>());
  }

  getAllBankAccountsAll(pageSize: number = 200): Observable<BankAccount[]> {
    return this.getAllBankAccounts(0, pageSize).pipe(
      expand((response: PageResponse<BankAccount>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllBankAccounts(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<BankAccount>) => response?.items ?? response ?? []),
      reduce((all: BankAccount[], chunk: BankAccount[]) => all.concat(chunk), [])
    );
  }

  getActiveBankAccounts(): Observable<BankAccount[]> {
    return this.http.get<any>(`${this.apiUrl}/active/list`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccount[]>());
  }

  updateBankAccount(id: number, bankAccount: BankAccount): Observable<BankAccount> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, bankAccount, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BankAccount>());
  }

  deleteBankAccount(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  deactivateBankAccount(id: number): Observable<SimpleStatusDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}/deactivate`, null, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  activateBankAccount(id: number): Observable<SimpleStatusDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}/activate`, null, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }
}
