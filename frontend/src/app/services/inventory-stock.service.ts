import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InventoryStock } from '../models/inventory-stock.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({
  providedIn: 'root'
})
export class InventoryStockService {
  private apiUrl = getApiUrl('/inventory');

  constructor(private http: HttpClient) { }

  getStock(id: number): Observable<InventoryStock> {
    return this.http.get<InventoryStock>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllStock(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getStockByVariant(variantId: number): Observable<InventoryStock> {
    return this.http.get<InventoryStock>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
