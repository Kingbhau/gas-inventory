import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { shareReplay, expand, map, reduce } from 'rxjs';
import { Supplier } from '../models/supplier.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private apiUrl = getApiUrl('/suppliers');
  private activeSuppliersCache$: Observable<Supplier[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  createSupplier(supplier: Supplier, businessId: number): Observable<Supplier> {
    const payload = { ...supplier, businessId };
    return this.http.post<any>(this.apiUrl, payload, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Supplier>());
  }

  getSupplier(id: number): Observable<Supplier> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Supplier>());
  }

  getAllSuppliers(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<Supplier>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Supplier>>());
  }

  getAllSuppliersAll(pageSize: number = 200): Observable<Supplier[]> {
    return this.getAllSuppliers(0, pageSize).pipe(
      expand((response: PageResponse<Supplier>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllSuppliers(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<Supplier>) => response?.items ?? response ?? []),
      reduce((all: Supplier[], chunk: Supplier[]) => all.concat(chunk), [])
    );
  }

  invalidateCache(): void {
    this.activeSuppliersCache$ = null;
    this.cacheService.invalidate(CACHE_KEYS.SUPPLIERS);
  }

  updateSupplier(id: number, supplier: Supplier): Observable<Supplier> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, supplier, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Supplier>());
  }

  deleteSupplier(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }
}
