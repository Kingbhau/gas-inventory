import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class CustomerCylinderLedgerService {
  private apiUrl = getApiUrl('/ledger');

  constructor(private http: HttpClient) {}

  // Get the current filled balance for a customer and variant
  getCustomerVariantBalance(customerId: number, variantId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}/balance`, { withCredentials: true });
  }
}
