import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { Sale } from '../models/sale.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class SaleService {
    getRecentSales(): Observable<any> {
      return this.http.get<any>(`${this.apiUrl}/recent`, { withCredentials: true })
        .pipe(timeout(30000));
    }
  private apiUrl = getApiUrl('/sales');

  constructor(private http: HttpClient) { }

  createSale(saleRequest: any): Observable<Sale> {
    return this.http.post<Sale>(this.apiUrl, saleRequest, { withCredentials: true })
      .pipe(timeout(30000));
  }

  getSale(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(timeout(30000));
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
    maxAmount?: number
  ): Observable<any> {
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
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(timeout(30000));
  }

  getSalesByCustomer(customerId: number, page: number = 0, size: number = 20): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}`, { params, withCredentials: true })
      .pipe(timeout(30000));
  }

  getSalesSummary(
    fromDate?: string,
    toDate?: string,
    customerId?: string,
    variantId?: number,
    minAmount?: number,
    maxAmount?: number
  ): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (customerId) params = params.set('customerId', customerId);
    if (variantId !== undefined && variantId !== null) params = params.set('variantId', variantId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true });
  }
}
