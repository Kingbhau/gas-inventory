import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { timeout, shareReplay } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { Customer } from '../models/customer.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS, CACHE_CONFIG } from './cache.service';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private apiUrl = getApiUrl('/customers');
  private activeCustomersCache$: Observable<Customer[]> | null = null;

  constructor(private http: HttpClient, private cacheService: CacheService) { }

  createCustomer(customer: Customer): Observable<Customer> {
    return this.http.post<any>(this.apiUrl, customer, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Customer>());
  }

  getCustomer(id: number): Observable<Customer> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Customer>());
  }

  getAllCustomers(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<PageResponse<Customer>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Customer>>());
  }

  getAllCustomersAll(pageSize: number = 200): Observable<Customer[]> {
    return this.getAllCustomers(0, pageSize).pipe(
      expand((response: PageResponse<Customer>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllCustomers(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<Customer>) => response?.items ?? response ?? []),
      reduce((all: Customer[], chunk: Customer[]) => all.concat(chunk), [])
    );
  }

  getActiveCustomers(): Observable<Customer[]> {
    // Return cached value if available
    const cached = this.cacheService.get<Customer[]>(CACHE_KEYS.CUSTOMERS);
    if (cached) {
      return new Observable(observer => {
        observer.next(cached);
        observer.complete();
      });
    }

    if (!this.activeCustomersCache$) {
      this.activeCustomersCache$ = this.http.get<any>(`${this.apiUrl}/active/list`, { withCredentials: true })
        .pipe(
          applyTimeout(),
          unwrapApiResponse<Customer[]>(),
          shareReplay(1)
        );
    }
    return this.activeCustomersCache$;
  }

  getActiveCustomersPaged(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'id',
    direction: string = 'ASC',
    search?: string,
    minDueAmount?: number
  ): Observable<PageResponse<Customer>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (typeof minDueAmount === 'number' && !isNaN(minDueAmount)) {
      params = params.set('minDueAmount', minDueAmount.toString());
    }
    return this.http.get<any>(`${this.apiUrl}/active`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Customer>>());
  }

  /**
   * Get all customers with caching. Invalidate cache after updates.
   */
  getAllCustomersWithCache(): Observable<Customer[]> {
    return this.cacheService.getOrLoad(
      CACHE_KEYS.CUSTOMERS,
      () => this.getActiveCustomers(),
      CACHE_CONFIG.REFERENCE_DATA
    );
  }

  /**
   * Invalidate customer cache after create/update/delete
   */
  invalidateCache(): void {
    this.activeCustomersCache$ = null;
    this.cacheService.invalidate(CACHE_KEYS.CUSTOMERS);
  }

  updateCustomer(id: number, customer: Customer): Observable<Customer> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, customer, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Customer>());
  }

  deleteCustomer(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  reactivateCustomer(id: number): Observable<Customer> {
    return this.http.post<any>(`${this.apiUrl}/${id}/reactivate`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Customer>());
  }
}
