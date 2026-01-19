import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Warehouse } from '../models/warehouse.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class WarehouseService {
  private apiUrl = getApiUrl('/warehouses');

  constructor(private http: HttpClient) { }

  /**
   * Get all warehouses
   */
  getAllWarehouses(): Observable<any> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true });
  }

  /**
   * Get only active warehouses
   */
  getActiveWarehouses(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/active`, { withCredentials: true });
  }

  /**
   * Get warehouse by ID
   */
  getWarehouseById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  /**
   * Get warehouse by name
   */
  getWarehouseByName(name: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/name/${name}`, { withCredentials: true });
  }

  /**
   * Create new warehouse
   */
  createWarehouse(name: string, businessId: number): Observable<any> {
    return this.http.post<any>(this.apiUrl, { name, businessId }, { withCredentials: true });
  }

  /**
   * Update warehouse
   */
  updateWarehouse(id: number, warehouse: Warehouse): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, warehouse, { withCredentials: true });
  }

  /**
   * Activate warehouse
   */
  activateWarehouse(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/activate`, {}, { withCredentials: true });
  }

  /**
   * Deactivate warehouse
   */
  deactivateWarehouse(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/deactivate`, {}, { withCredentials: true });
  }
}
