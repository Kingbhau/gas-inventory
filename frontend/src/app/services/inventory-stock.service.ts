import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
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

  getAllStockAll(pageSize: number = 200): Observable<any[]> {
    return this.getAllStock(0, pageSize, 'id', 'ASC').pipe(
      expand((response: any) => {
        const currentPage = response?.number ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllStock(nextPage, pageSize, 'id', 'ASC') : EMPTY;
      }),
      map((response: any) => response?.content ?? response ?? []),
      reduce((all: any[], chunk: any[]) => all.concat(chunk), [])
    );
  }

  getStockByVariant(variantId: number): Observable<InventoryStock> {
    return this.http.get<InventoryStock>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getStockByWarehouse(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getInventoryByWarehouse(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  setupWarehouseInventory(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/setup`, payload, { withCredentials: true })
      .pipe(applyTimeout());
  }

  transferStock(transferRequest: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/transfer`, transferRequest, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
