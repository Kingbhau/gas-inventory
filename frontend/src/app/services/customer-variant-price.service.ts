import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomerVariantPrice } from '../models/customer-variant-price.model';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class CustomerVariantPriceService {
  private apiUrl = getApiUrl('/customers');

  constructor(private http: HttpClient) {}

  /**
   * Create pricing for a customer-variant combination
   */
  createPrice(customerId: number, price: CustomerVariantPrice): Observable<CustomerVariantPrice> {
    return this.http.post<any>(
      `${this.apiUrl}/${customerId}/variant-prices`,
      price,
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<CustomerVariantPrice>());
  }

  /**
   * Get pricing for a specific customer-variant combination
   */
  getPriceByVariant(customerId: number, variantId: number): Observable<CustomerVariantPrice> {
    return this.http.get<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`,
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<CustomerVariantPrice>());
  }

  /**
   * Get all variant prices for a customer
   */
  getPricesByCustomer(customerId: number): Observable<CustomerVariantPrice[]> {
    return this.http.get<any>(
      `${this.apiUrl}/${customerId}/variant-prices`,
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<CustomerVariantPrice[]>());
  }

  /**
   * Update pricing for a customer-variant combination
   */
  updatePrice(customerId: number, variantId: number, price: CustomerVariantPrice): Observable<CustomerVariantPrice> {
    return this.http.put<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`,
      price,
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<CustomerVariantPrice>());
  }

  /**
   * Delete pricing for a customer-variant combination
   */
  deletePrice(customerId: number, variantId: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`,
      { withCredentials: true }
    ).pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  /**
   * Get pricing for sale entry (customer + variant)
   */
  getPricingForSale(customerId: number, variantId: number): Observable<CustomerVariantPrice> {
    return this.getPriceByVariant(customerId, variantId);
  }
}
