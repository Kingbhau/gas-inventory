import { CustomerBalance } from '../models/customer-balance.model';

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomerCylinderLedger } from '../models/customer-cylinder-ledger.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CustomerCylinderLedgerService {
      getAllMovements(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/movements`, { withCredentials: true });
      }
    constructor(private http: HttpClient) { }

    // Rename to getAllReturnPendingSummary for clarity
    getAllReturnPendingSummary(): Observable<any[]> {
      return this.http.get<any[]>(`${this.apiUrl}/pending-summary`, { withCredentials: true });
    }
  private apiUrl = getApiUrl('/ledger');



  getLedgerEntry(id: number): Observable<CustomerCylinderLedger> {
    return this.http.get<CustomerCylinderLedger>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  getAllLedger(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true });
  }

  getLedgerByCustomer(customerId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/customer/${customerId}`, { withCredentials: true });
  }

  getLedgerByCustomerPaginated(customerId: number, page: number = 0, size: number = 5, sortBy: string = 'id', direction: string = 'DESC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/paginated`, { params, withCredentials: true });
  }
    /**
   * Get balances for a page of customers (all variants per customer)
   */
  getCustomerBalances(page: number, size: number): Observable<CustomerBalance[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<CustomerBalance[]>(`${this.apiUrl}/customer-balances`, { params, withCredentials: true });
  }

  getLedgerByCustomerAndVariant(customerId: number, variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}`, { withCredentials: true });
  }

  getLedgerByVariant(variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true });
  }

  getBalance(customerId: number, variantId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}/balance`, { withCredentials: true });
  }
  /**
   * Record empty cylinder return (no sale)
   */
  recordEmptyReturn(data: {
    customerId: number|string,
    variantId: number|string,
    emptyIn: number,
    transactionDate: string
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/empty-return`, data, { withCredentials: true });
  }
}
