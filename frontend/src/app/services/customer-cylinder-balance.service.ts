import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { map } from 'rxjs/operators';
import { CustomerDueAmount } from '../models/customer-due-amount.model';

@Injectable({ providedIn: 'root' })
export class CustomerCylinderLedgerService {
  private apiUrl = getApiUrl('/ledger');

  constructor(private http: HttpClient) {}

  // Get the current filled balance for a customer and variant
  getCustomerVariantBalance(customerId: number, variantId: number): Observable<number> {
    return this.http.get<any>(`${this.apiUrl}/customer/${customerId}/variant/${variantId}/balance`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<number>());
  }

  // Get current due amounts for one or more customers
  getCustomerDueAmounts(customerIds: number[]): Observable<{ [key: number]: number }> {
    return this.http.post<any>(`${this.apiUrl}/customer-due-amounts`, { customerIds }, { withCredentials: true })
      .pipe(
        applyTimeout(),
        unwrapApiResponse<CustomerDueAmount[]>(),
        map((items: CustomerDueAmount[]) => {
          const result: { [key: number]: number } = {};
          (items || []).forEach((item) => {
            if (item && item.customerId !== undefined) {
              result[item.customerId] = item.dueAmount ?? 0;
            }
          });
          return result;
        })
      );
  }
}
