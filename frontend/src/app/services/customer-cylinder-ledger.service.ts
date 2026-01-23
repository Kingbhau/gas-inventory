import { CustomerBalance } from '../models/customer-balance.model';

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomerCylinderLedger } from '../models/customer-cylinder-ledger.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({
  providedIn: 'root'
})
export class CustomerCylinderLedgerService {
      getAllMovements(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/movements`, { withCredentials: true })
          .pipe(applyTimeout());
      }

      getMovementsByWarehouse(warehouseId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/movements/warehouse/${warehouseId}`, { withCredentials: true })
          .pipe(applyTimeout());
      }
    constructor(private http: HttpClient) { }

    // Rename to getAllReturnPendingSummary for clarity
    getAllReturnPendingSummary(): Observable<any[]> {
      return this.http.get<any[]>(`${this.apiUrl}/pending-summary`, { withCredentials: true })
        .pipe(applyTimeout());
    }
  private apiUrl = getApiUrl('/ledger');



  getLedgerEntry(id: number): Observable<CustomerCylinderLedger> {
    return this.http.get<CustomerCylinderLedger>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllLedger(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getLedgerByCustomer(customerId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/customer/${customerId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getLedgerByCustomerPaginated(customerId: number, page: number = 0, size: number = 5, sortBy: string = 'id', direction: string = 'DESC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/paginated`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }
    /**
   * Get balances for a page of customers (all variants per customer)
   */
  getCustomerBalances(page: number, size: number): Observable<CustomerBalance[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<CustomerBalance[]>(`${this.apiUrl}/customer-balances`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getLedgerByCustomerAndVariant(customerId: number, variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getLedgerByVariant(variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<CustomerCylinderLedger[]>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getBalance(customerId: number, variantId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}/balance`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Record a payment transaction
   */
  recordPayment(data: {
    customerId: number,
    amount: number,
    paymentDate: string
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/payment`, data, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Get complete summary for a customer (across all ledger entries)
   */
  getCustomerLedgerSummary(customerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/customer/${customerId}/summary`, { withCredentials: true })
      .pipe(applyTimeout());
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
    return this.http.post(`${this.apiUrl}/empty-return`, data, { withCredentials: true })
      .pipe(applyTimeout());
  }

  /**
   * Update a ledger entry with full chain recalculation
   * Validates that no due amounts go negative anywhere in the chain
   */
  updateLedgerEntry(ledgerId: number, updateData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${ledgerId}`, updateData, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
