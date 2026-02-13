import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, map, tap } from 'rxjs';
import { Warehouse } from '../models/warehouse.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class WarehouseService {
  private apiUrl = getApiUrl('/warehouses');
  private activeWarehousesCache$: Observable<Warehouse[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  /**
   * Get all warehouses
   */
  getAllWarehouses(): Observable<Warehouse[]> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse[]>());
  }

  /**
   * Get only active warehouses with caching
   */
  getActiveWarehouses(): Observable<Warehouse[]> {
    // Return cached value if available
    const cached = this.cacheService.get<Warehouse[]>(CACHE_KEYS.WAREHOUSES);
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    if (!this.activeWarehousesCache$) {
      this.activeWarehousesCache$ = this.http.get<any>(`${this.apiUrl}/active`, { withCredentials: true })
        .pipe(
          applyTimeout(),
          unwrapApiResponse<Warehouse[]>(),
          tap(data => {
            // Cache the data for future use
            this.cacheService.set(CACHE_KEYS.WAREHOUSES, data, CACHE_CONFIG.REFERENCE_DATA);
          }),
          shareReplay(1)
        );
    }
    return this.activeWarehousesCache$;
  }

  /**
   * Get warehouse by ID
   */
  getWarehouseById(id: number): Observable<Warehouse> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }

  /**
   * Get warehouse by name
   */
  getWarehouseByName(name: string): Observable<Warehouse> {
    return this.http.get<any>(`${this.apiUrl}/name/${name}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }

  /**
   * Invalidate warehouse cache
   */
  invalidateCache(): void {
    this.activeWarehousesCache$ = null;
    this.cacheService.invalidate(CACHE_KEYS.WAREHOUSES);
  }

  /**
   * Create new warehouse
   */
  createWarehouse(name: string, businessId: number): Observable<Warehouse> {
    return this.http.post<any>(this.apiUrl, { name, businessId }, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }

  /**
   * Update warehouse
   */
  updateWarehouse(id: number, warehouse: Warehouse): Observable<Warehouse> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, warehouse, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }

  /**
   * Activate warehouse
   */
  activateWarehouse(id: number): Observable<Warehouse> {
    return this.http.put<any>(`${this.apiUrl}/${id}/activate`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }

  /**
   * Deactivate warehouse
   */
  deactivateWarehouse(id: number): Observable<Warehouse> {
    return this.http.put<any>(`${this.apiUrl}/${id}/deactivate`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Warehouse>());
  }
}
