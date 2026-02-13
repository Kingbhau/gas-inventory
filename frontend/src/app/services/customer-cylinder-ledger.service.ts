import { CustomerBalance } from '../models/customer-balance.model';
import { CustomerDueAmount } from '../models/customer-due-amount.model';
import { CustomerLedgerSummary } from '../models/customer-ledger-summary.model';
import { LedgerUpdateRequest } from '../models/ledger-update-request.model';
import { PageResponse } from '../models/page-response';
import { PaymentsSummary } from '../models/payments-summary.model';

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { CustomerCylinderLedger } from '../models/customer-cylinder-ledger.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class CustomerCylinderLedgerService {
      getAllMovements(): Observable<CustomerCylinderLedger[]> {
        return this.http.get<any>(`${this.apiUrl}/movements`, { withCredentials: true })
          .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
      }

      getAllMovementsPaged(
        page: number = 0,
        size: number = 20,
        sortBy: string = 'transactionDate',
        direction: string = 'DESC',
        variantId?: number | null,
        refType?: string | null,
        includeTransfers: boolean = true
      ): Observable<PageResponse<CustomerCylinderLedger>> {
        let params = new HttpParams()
          .set('page', page.toString())
          .set('size', size.toString())
          .set('sortBy', sortBy)
          .set('direction', direction);
        if (variantId) params = params.set('variantId', variantId.toString());
        if (refType) params = params.set('refType', refType);
        params = params.set('includeTransfers', includeTransfers.toString());
        return this.http.get<any>(`${this.apiUrl}/movements/paged`, { params, withCredentials: true })
          .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
      }

      getMovementsByWarehouse(warehouseId: number): Observable<CustomerCylinderLedger[]> {
        return this.http.get<any>(`${this.apiUrl}/movements/warehouse/${warehouseId}`, { withCredentials: true })
          .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
      }

      getMovementsByWarehousePaged(
        warehouseId: number,
        page: number = 0,
        size: number = 20,
        sortBy: string = 'transactionDate',
        direction: string = 'DESC',
        variantId?: number | null,
        refType?: string | null,
        includeTransfers: boolean = true
      ): Observable<PageResponse<CustomerCylinderLedger>> {
        let params = new HttpParams()
          .set('page', page.toString())
          .set('size', size.toString())
          .set('sortBy', sortBy)
          .set('direction', direction);
        if (variantId) params = params.set('variantId', variantId.toString());
        if (refType) params = params.set('refType', refType);
        params = params.set('includeTransfers', includeTransfers.toString());
        return this.http.get<any>(`${this.apiUrl}/movements/warehouse/${warehouseId}/paged`, { params, withCredentials: true })
          .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
      }
    constructor(private http: HttpClient) { }

    // Rename to getAllReturnPendingSummary for clarity
    getAllReturnPendingSummary(): Observable<CustomerCylinderLedger[]> {
      return this.http.get<any>(`${this.apiUrl}/pending-summary`, { withCredentials: true })
        .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
    }
  private apiUrl = getApiUrl('/ledger');



  getLedgerEntry(id: number): Observable<CustomerCylinderLedger> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger>());
  }

  getAllLedger(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<CustomerCylinderLedger>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
  }

  getLedgerByCustomer(customerId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
  }

  getLedgerByCustomerPaginated(customerId: number, page: number = 0, size: number = 5, sortBy: string = 'id', direction: string = 'DESC'): Observable<PageResponse<CustomerCylinderLedger>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/paginated`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
  }

  getLedgerByCustomerAll(customerId: number, pageSize: number = 200): Observable<CustomerCylinderLedger[]> {
    return this.getLedgerByCustomerPaginated(customerId, 0, pageSize, 'id', 'DESC').pipe(
      expand((response: PageResponse<CustomerCylinderLedger>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages
          ? this.getLedgerByCustomerPaginated(customerId, nextPage, pageSize, 'id', 'DESC')
          : EMPTY;
      }),
      map((response: PageResponse<CustomerCylinderLedger>) => response?.items ?? response ?? []),
      reduce((all: CustomerCylinderLedger[], chunk: CustomerCylinderLedger[]) => all.concat(chunk), [])
    );
  }
    /**
   * Get balances for a page of customers (all variants per customer)
   */
  getCustomerBalances(page: number, size: number): Observable<CustomerBalance[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/customer-balances`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerBalance[]>());
  }

  getCustomerDueAmounts(customerIds: number[]): Observable<{ [key: number]: number }> {
    return this.http.post<any>(`${this.apiUrl}/customer-due-amounts`, { customerIds }, { withCredentials: true })
      .pipe(
        applyTimeout(),
        unwrapApiResponse<CustomerDueAmount[]>(),
        map((items: CustomerDueAmount[]) => {
          const result: { [key: number]: number } = {};
          (items || []).forEach((item) => {
            if (item && item.customerId !== undefined) {
              result[item.customerId] = item.dueAmount ?? 0;
            }
          });
          return result;
        })
      );
  }

  getLedgerByCustomerAndVariant(customerId: number, variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
  }

  getLedgerByVariant(variantId: number): Observable<CustomerCylinderLedger[]> {
    return this.http.get<any>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger[]>());
  }

  getBalance(customerId: number, variantId: number): Observable<number> {
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}/balance`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<number>());
  }

  /**
   * Record a payment transaction
   */
  recordPayment(data: {
    customerId: number,
    amount: number,
    paymentDate: string
  }): Observable<CustomerCylinderLedger> {
    return this.http.post<any>(`${this.apiUrl}/payment`, data, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger>());
  }

  /**
   * Get complete summary for a customer (across all ledger entries)
   */
  getCustomerLedgerSummary(customerId: number): Observable<CustomerLedgerSummary> {
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/summary`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerLedgerSummary>());
  }

  /**
   * Record empty cylinder return (no sale)
   */
  recordEmptyReturn(data: {
    customerId: number|string,
    variantId: number|string,
    emptyIn: number,
    transactionDate: string
  }): Observable<CustomerCylinderLedger> {
    return this.http.post<any>(`${this.apiUrl}/empty-return`, data, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger>());
  }

  /**
   * Get empty returns with filtering
   */
  getEmptyReturns(
    page: number = 0,
    pageSize: number = 10,
    sortBy: string = 'transactionDate',
    direction: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    variantId?: string,
    createdBy?: string
  ): Observable<PageResponse<CustomerCylinderLedger>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (variantId) params = params.set('variantId', variantId);
    if (createdBy) params = params.set('createdBy', createdBy);

    return this.http.get<any>(`${this.apiUrl}/empty-returns`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
  }

  getPayments(
    page: number = 0,
    pageSize: number = 10,
    sortBy: string = 'transactionDate',
    direction: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    customerId?: number,
    paymentMode?: string,
    bankAccountId?: number,
    createdBy?: string
  ): Observable<PageResponse<CustomerCylinderLedger>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId.toString());
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId.toString());
    if (createdBy) params = params.set('createdBy', createdBy);

    return this.http.get<any>(`${this.apiUrl}/payments`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CustomerCylinderLedger>>());
  }

  getPaymentsSummary(
    fromDate?: string,
    toDate?: string,
    customerId?: number,
    paymentMode?: string,
    bankAccountId?: number,
    createdBy?: string
  ): Observable<PaymentsSummary> {
    let params = new HttpParams();

    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId.toString());
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId.toString());
    if (createdBy) params = params.set('createdBy', createdBy);

    return this.http.get<any>(`${this.apiUrl}/payments-summary`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PaymentsSummary>());
  }

  /**
   * Update a ledger entry with full chain recalculation
   * Validates that no due amounts go negative anywhere in the chain
   */
  updateLedgerEntry(ledgerId: number, updateData: LedgerUpdateRequest): Observable<CustomerCylinderLedger> {
    return this.http.put<any>(`${this.apiUrl}/${ledgerId}`, updateData, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<CustomerCylinderLedger>());
  }
}
