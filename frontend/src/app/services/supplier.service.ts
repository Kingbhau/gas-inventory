import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Supplier } from '../models/supplier.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private apiUrl = getApiUrl('/suppliers');

  constructor(private http: HttpClient) { }

  createSupplier(supplier: Supplier): Observable<Supplier> {
    return this.http.post<Supplier>(this.apiUrl, supplier, { withCredentials: true });
  }

  getSupplier(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  getAllSuppliers(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true });
  }

  updateSupplier(id: number, supplier: Supplier): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/${id}`, supplier, { withCredentials: true });
  }

  deleteSupplier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
