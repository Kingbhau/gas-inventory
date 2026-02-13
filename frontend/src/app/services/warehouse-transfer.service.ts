import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WarehouseTransfer } from '../models/warehouse-transfer.model';
import { PageResponse } from '../models/page-response';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

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
  transferCylinders(transfer: WarehouseTransfer): Observable<WarehouseTransfer> {
    return this.http.post<any>(this.apiUrl, transfer, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer>());
  }

  /**
   * Get all warehouse transfers (audit trail)
   */
  getAllTransfers(): Observable<WarehouseTransfer[]> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer[]>());
  }

  /**
   * Get paged warehouse transfers (audit trail)
   */
  getAllTransfersPaged(page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<PageResponse<WarehouseTransfer>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/paged`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<WarehouseTransfer>>());
  }

  /**
   * Get transfer by ID
   */
  getTransferById(id: number): Observable<WarehouseTransfer> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer>());
  }

  /**
   * Get transfers for a warehouse (incoming + outgoing)
   */
  getTransfersForWarehouse(warehouseId: number): Observable<WarehouseTransfer[]> {
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer[]>());
  }

  getTransfersForWarehousePaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<PageResponse<WarehouseTransfer>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/warehouse/${warehouseId}/paged`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<WarehouseTransfer>>());
  }

  /**
   * Get outgoing transfers from warehouse
   */
  getTransfersFrom(warehouseId: number): Observable<WarehouseTransfer[]> {
    return this.http.get<any>(`${this.apiUrl}/from/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer[]>());
  }

  getTransfersFromPaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<PageResponse<WarehouseTransfer>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/from/${warehouseId}/paged`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<WarehouseTransfer>>());
  }

  /**
   * Get incoming transfers to warehouse
   */
  getTransfersTo(warehouseId: number): Observable<WarehouseTransfer[]> {
    return this.http.get<any>(`${this.apiUrl}/to/${warehouseId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer[]>());
  }

  getTransfersToPaged(warehouseId: number, page: number = 0, size: number = 20, sortBy: string = 'transferDate', direction: string = 'DESC'): Observable<PageResponse<WarehouseTransfer>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(`${this.apiUrl}/to/${warehouseId}/paged`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<WarehouseTransfer>>());
  }

  /**
   * Get transfers between two specific warehouses
   */
  getTransfersBetweenWarehouses(fromId: number, toId: number): Observable<WarehouseTransfer[]> {
    return this.http.get<any>(`${this.apiUrl}/between/${fromId}/${toId}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<WarehouseTransfer[]>());
  }
}
