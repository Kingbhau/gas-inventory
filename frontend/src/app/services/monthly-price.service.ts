import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MonthlyPrice } from '../models/monthly-price.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class MonthlyPriceService {
  private apiUrl = getApiUrl('/monthly-prices');

  constructor(private http: HttpClient) { }

  createPrice(price: MonthlyPrice): Observable<MonthlyPrice> {
    return this.http.post<MonthlyPrice>(this.apiUrl, price, { withCredentials: true });
  }

  getPrice(id: number): Observable<MonthlyPrice> {
    return this.http.get<MonthlyPrice>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  getAllPrices(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true });
  }

  getPricesByVariant(variantId: number): Observable<MonthlyPrice[]> {
    return this.http.get<MonthlyPrice[]>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true });
  }

  getPriceForMonth(variantId: number, monthYear: string): Observable<MonthlyPrice> {
    return this.http.get<MonthlyPrice>(`${this.apiUrl}/variant/${variantId}/month/${monthYear}`, { withCredentials: true });
  }

  getLatestPriceForMonth(variantId: number, monthYear: string): Observable<MonthlyPrice> {
    return this.http.get<MonthlyPrice>(`${this.apiUrl}/variant/${variantId}/latest/${monthYear}`, { withCredentials: true });
  }

  updatePrice(id: number, price: MonthlyPrice): Observable<MonthlyPrice> {
    return this.http.put<MonthlyPrice>(`${this.apiUrl}/${id}`, price, { withCredentials: true });
  }

  deletePrice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
