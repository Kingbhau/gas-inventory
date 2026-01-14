import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs';
import { Customer } from '../models/customer.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
}

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private apiUrl = getApiUrl('/customers');

  constructor(private http: HttpClient) { }

  createCustomer(customer: Customer): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, customer, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getCustomer(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllCustomers(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getActiveCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.apiUrl}/active/list`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  updateCustomer(id: number, customer: Customer): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/${id}`, customer, { withCredentials: true })
      .pipe(applyTimeout());
  }

  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }
}
