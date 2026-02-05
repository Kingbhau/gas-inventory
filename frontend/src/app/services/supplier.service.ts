import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { shareReplay, expand, map, reduce } from 'rxjs';
import { Supplier } from '../models/supplier.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private apiUrl = getApiUrl('/suppliers');
  private activeSuppliersCache$: Observable<Supplier[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  createSupplier(supplier: Supplier, businessId: number): Observable<Supplier> {
    const payload = { ...supplier, businessId };
    return this.http.post<Supplier>(this.apiUrl, payload, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getSupplier(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllSuppliers(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllSuppliersAll(pageSize: number = 200): Observable<Supplier[]> {
    return this.getAllSuppliers(0, pageSize).pipe(
      expand((response: any) => {
        const currentPage = response?.number ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllSuppliers(nextPage, pageSize) : EMPTY;
      }),
      map((response: any) => response?.content ?? response ?? []),
      reduce((all: Supplier[], chunk: Supplier[]) => all.concat(chunk), [])
    );
  }

  invalidateCache(): void {
    this.activeSuppliersCache$ = null;
    this.cacheService.invalidate(CACHE_KEYS.SUPPLIERS);
  }

  updateSupplier(id: number, supplier: Supplier): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/${id}`, supplier, { withCredentials: true });
  }

  deleteSupplier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
