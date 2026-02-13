import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MonthlyPrice } from '../models/monthly-price.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class MonthlyPriceService {
  private apiUrl = getApiUrl('/monthly-prices');

  constructor(private http: HttpClient) { }

  createPrice(price: MonthlyPrice): Observable<MonthlyPrice> {
    return this.http.post<any>(this.apiUrl, price, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice>());
  }

  getPrice(id: number): Observable<MonthlyPrice> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice>());
  }

  getAllPrices(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<MonthlyPrice>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<MonthlyPrice>>());
  }

  getPricesByVariant(variantId: number): Observable<MonthlyPrice[]> {
    return this.http.get<any>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice[]>());
  }

  getPriceForMonth(variantId: number, monthYear: string): Observable<MonthlyPrice> {
    return this.http.get<any>(`${this.apiUrl}/variant/${variantId}/month/${monthYear}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice>());
  }

  getLatestPriceForMonth(variantId: number, monthYear: string): Observable<MonthlyPrice> {
    return this.http.get<any>(`${this.apiUrl}/variant/${variantId}/latest/${monthYear}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice>());
  }

  updatePrice(id: number, price: MonthlyPrice): Observable<MonthlyPrice> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, price, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<MonthlyPrice>());
  }

  deletePrice(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }
}
