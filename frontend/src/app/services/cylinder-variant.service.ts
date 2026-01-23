import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs';
import { CylinderVariant } from '../models/cylinder-variant.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';

@Injectable({
  providedIn: 'root'
})
export class CylinderVariantService {
  private apiUrl = getApiUrl('/variants');
  private activeVariantsCache$: Observable<CylinderVariant[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  createVariant(variant: CylinderVariant): Observable<CylinderVariant> {
    return this.http.post<CylinderVariant>(this.apiUrl, variant)
      .pipe(applyTimeout());
  }

  getVariant(id: number): Observable<CylinderVariant> {
    return this.http.get<CylinderVariant>(`${this.apiUrl}/${id}`)
      .pipe(applyTimeout());
  }

  getAllVariants(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params })
      .pipe(applyTimeout());
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
      this.activeVariantsCache$ = this.http.get<CylinderVariant[]>(`${this.apiUrl}/active/list`)
        .pipe(shareReplay(1));
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
    return this.http.put<CylinderVariant>(`${this.apiUrl}/${id}`, variant);
  }

  deleteVariant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  reactivateVariant(id: number): Observable<CylinderVariant> {
    return this.http.post<CylinderVariant>(`${this.apiUrl}/${id}/reactivate`, {});
  }
}
