import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomerVariantPrice } from '../models/customer-variant-price.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CustomerVariantPriceService {
  private apiUrl = getApiUrl('/customers');

  constructor(private http: HttpClient) {}

  /**
   * Create pricing for a customer-variant combination
   */
  createPrice(customerId: number, price: CustomerVariantPrice): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/${customerId}/variant-prices`,
      price
    );
  }

  /**
   * Get pricing for a specific customer-variant combination
   */
  getPriceByVariant(customerId: number, variantId: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`
    );
  }

  /**
   * Get all variant prices for a customer
   */
  getPricesByCustomer(customerId: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/${customerId}/variant-prices`
    );
  }

  /**
   * Update pricing for a customer-variant combination
   */
  updatePrice(customerId: number, variantId: number, price: CustomerVariantPrice): Observable<any> {
    return this.http.put<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`,
      price
    );
  }

  /**
   * Delete pricing for a customer-variant combination
   */
  deletePrice(customerId: number, variantId: number): Observable<any> {
    return this.http.delete<any>(
      `${this.apiUrl}/${customerId}/variant-prices/${variantId}`
    );
  }

  /**
   * Get pricing for sale entry (customer + variant)
   */
  getPricingForSale(customerId: number, variantId: number): Observable<CustomerVariantPrice> {
    return this.getPriceByVariant(customerId, variantId);
  }
}
