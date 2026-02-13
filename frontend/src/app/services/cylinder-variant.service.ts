import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { shareReplay, expand, map, reduce } from 'rxjs';
import { CylinderVariant } from '../models/cylinder-variant.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class CylinderVariantService {
  private apiUrl = getApiUrl('/variants');
  private activeVariantsCache$: Observable<CylinderVariant[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  createVariant(variant: CylinderVariant): Observable<CylinderVariant> {
    return this.http.post<any>(this.apiUrl, variant)
      .pipe(applyTimeout(), unwrapApiResponse<CylinderVariant>());
  }

  getVariant(id: number): Observable<CylinderVariant> {
    return this.http.get<any>(`${this.apiUrl}/${id}`)
      .pipe(applyTimeout(), unwrapApiResponse<CylinderVariant>());
  }

  getAllVariants(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<CylinderVariant>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<CylinderVariant>>());
  }

  getAllVariantsAll(pageSize: number = 200): Observable<CylinderVariant[]> {
    return this.getAllVariants(0, pageSize).pipe(
      expand((response: PageResponse<CylinderVariant>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllVariants(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<CylinderVariant>) => response?.items ?? response ?? []),
      reduce((all: CylinderVariant[], chunk: CylinderVariant[]) => all.concat(chunk), [])
    );
  }

  getActiveVariants(): Observable<CylinderVariant[]> {
    // Return cached value if available
    const cached = this.cacheService.get<CylinderVariant[]>(CACHE_KEYS.VARIANTS);
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    if (!this.activeVariantsCache$) {
      this.activeVariantsCache$ = this.http.get<any>(`${this.apiUrl}/active/list`)
        .pipe(applyTimeout(), unwrapApiResponse<CylinderVariant[]>(), shareReplay(1));
    }
    return this.activeVariantsCache$;
  }

  /**
   * Get all variants with caching
   */
  getAllVariantsWithCache(): Observable<CylinderVariant[]> {
    return this.cacheService.getOrLoad(
      CACHE_KEYS.VARIANTS,
      () => this.getActiveVariants(),
      CACHE_CONFIG.REFERENCE_DATA
    );
  }

  /**
   * Invalidate variant cache after create/update/delete
   */
  invalidateCache(): void {
    this.activeVariantsCache$ = null;
    this.cacheService.invalidate(CACHE_KEYS.VARIANTS);
  }

  updateVariant(id: number, variant: CylinderVariant): Observable<CylinderVariant> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, variant)
      .pipe(applyTimeout(), unwrapApiResponse<CylinderVariant>());
  }

  deleteVariant(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`)
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  reactivateVariant(id: number): Observable<CylinderVariant> {
    return this.http.post<any>(`${this.apiUrl}/${id}/reactivate`, {})
      .pipe(applyTimeout(), unwrapApiResponse<CylinderVariant>());
  }
}
