import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WarehouseTransfer } from '../models/warehouse-transfer.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class WarehouseTransferService {
  private apiUrl = getApiUrl('/warehouse-transfers');

  constructor(private http: HttpClient) { }

  /**
   * Create warehouse transfer (atomic operation)
   * Handles validation, stock checks, and concurrency control
   */
  transferCylinders(transfer: WarehouseTransfer): Observable<any> {
    return this.http.post<any>(this.apiUrl, transfer, { withCredentials: true });
  }

  /**
   * Get all warehouse transfers (audit trail)
   */
  getAllTransfers(): Observable<any> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true });
  }

  /**
   * Get paged warehouse transfers (audit trail)
   */
  getAllTransfersPaged(page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/paged`, { params, withCredentials: true });
  }

  /**
   * Get transfer by ID
   */
  getTransferById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  /**
   * Get transfers for a warehouse (incoming + outgoing)
   */
  getTransfersForWarehouse(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true });
  }

  getTransfersForWarehousePaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}/paged`, { params, withCredentials: true });
  }

  /**
   * Get outgoing transfers from warehouse
   */
  getTransfersFrom(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/from/${warehouseId}`, { withCredentials: true });
  }

  getTransfersFromPaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/from/${warehouseId}/paged`, { params, withCredentials: true });
  }

  /**
   * Get incoming transfers to warehouse
   */
  getTransfersTo(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/to/${warehouseId}`, { withCredentials: true });
  }

  getTransfersToPaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/to/${warehouseId}/paged`, { params, withCredentials: true });
  }

  /**
   * Get transfers between two specific warehouses
   */
  getTransfersBetweenWarehouses(fromId: number, toId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/between/${fromId}/${toId}`, { withCredentials: true });
  }
}
