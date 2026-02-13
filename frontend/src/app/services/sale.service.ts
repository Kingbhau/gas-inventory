import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { Sale } from '../models/sale.model';
import { CreateSaleRequest } from '../models/create-sale-request.model';
import { PageResponse } from '../models/page-response';
import { PaymentModeSummary } from '../models/payment-mode-summary.model';
import { SaleSummary } from '../models/sale-summary.model';
import { getApiUrl } from '../config/api.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class SaleService {
    getRecentSales(): Observable<Sale[]> {
      return this.http.get<any>(`${this.apiUrl}/recent`, { withCredentials: true })
        .pipe(timeout(30000), unwrapApiResponse<Sale[]>());
    }
  private apiUrl = getApiUrl('/sales');

  constructor(private http: HttpClient) { }

  createSale(saleRequest: CreateSaleRequest): Observable<Sale> {
    return this.http.post<any>(this.apiUrl, saleRequest, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<Sale>());
  }

  getSale(id: number): Observable<Sale> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<Sale>());
  }

  getAllSales(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'saleDate',
    direction: string = 'DESC',
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    variantId?: number,
    minAmount?: number,
    maxAmount?: number,
    referenceNumber?: string,
    createdBy?: string
  ): Observable<PageResponse<Sale>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (variantId !== undefined && variantId !== null) params = params.set('variantId', variantId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);
    if (createdBy) params = params.set('createdBy', createdBy);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<PageResponse<Sale>>());
  }

  getSalesByCustomer(customerId: number, page: number = 0, size: number = 20): Observable<PageResponse<Sale>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}`, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<PageResponse<Sale>>());
  }

  getSalesSummary(
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    variantId?: number,
    minAmount?: number,
    maxAmount?: number,
    referenceNumber?: string,
    createdBy?: string
  ): Observable<SaleSummary> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (variantId !== undefined && variantId !== null) params = params.set('variantId', variantId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    if (referenceNumber) params = params.set('referenceNumber', referenceNumber);
    if (createdBy) params = params.set('createdBy', createdBy);
    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<SaleSummary>());
  }

  getPaymentModeSummary(
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    paymentMode?: string,
    variantId?: number,
    bankAccountId?: number,
    minAmount?: number,
    maxAmount?: number,
    minTransactionCount?: number
  ): Observable<PaymentModeSummary> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (variantId !== undefined && variantId !== null) params = params.set('variantId', variantId.toString());
    if (bankAccountId !== undefined && bankAccountId !== null) params = params.set('bankAccountId', bankAccountId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    if (minTransactionCount !== undefined && minTransactionCount !== null) params = params.set('minTransactionCount', minTransactionCount.toString());
    return this.http.get<any>(`${this.apiUrl}/payment-mode-summary`, { params, withCredentials: true })
      .pipe(timeout(30000), unwrapApiResponse<PaymentModeSummary>());
  }
}
