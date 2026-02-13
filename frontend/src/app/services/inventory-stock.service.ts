import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { InventoryStock } from '../models/inventory-stock.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { WarehouseTransferDTO } from '../models/warehouse-transfer.model';
import { WarehouseInventorySetupRequest } from '../models/warehouse-inventory.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class InventoryStockService {
  private apiUrl = getApiUrl('/inventory');

  constructor(private http: HttpClient) { }

  getStock(id: number): Observable<InventoryStock> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<InventoryStock>());
  }

  getAllStock(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<InventoryStock>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<InventoryStock>>());
  }

  getAllStockAll(pageSize: number = 200): Observable<InventoryStock[]> {
    return this.getAllStock(0, pageSize, 'id', 'ASC').pipe(
      expand((response: PageResponse<InventoryStock>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllStock(nextPage, pageSize, 'id', 'ASC') : EMPTY;
      }),
      map((response: PageResponse<InventoryStock>) => response?.items ?? response ?? []),
      reduce((all: InventoryStock[], chunk: InventoryStock[]) => all.concat(chunk), [])
    );
  }

  getStockByVariant(variantId: number): Observable<InventoryStock> {
    return this.http.get<any>(`${this.apiUrl}/variant/${variantId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<InventoryStock>());
  }

  getStockByWarehouse(warehouseId: number): Observable<InventoryStock[]> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<InventoryStock[]>());
  }

  getInventoryByWarehouse(warehouseId: number): Observable<InventoryStock[]> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<InventoryStock[]>());
  }

  setupWarehouseInventory(payload: WarehouseInventorySetupRequest): Observable<SimpleStatusDTO> {
    return this.http.post<any>(`${this.apiUrl}/setup`, payload, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  transferStock(transferRequest: WarehouseTransferDTO): Observable<WarehouseTransferDTO> {
    return this.http.post<any>(`${this.apiUrl}/transfer`, transferRequest, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransferDTO>());
  }
}
