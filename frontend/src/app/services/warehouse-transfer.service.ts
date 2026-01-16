import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

  /**
   * Get outgoing transfers from warehouse
   */
  getTransfersFrom(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/from/${warehouseId}`, { withCredentials: true });
  }

  /**
   * Get incoming transfers to warehouse
   */
  getTransfersTo(warehouseId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/to/${warehouseId}`, { withCredentials: true });
  }

  /**
   * Get transfers between two specific warehouses
   */
  getTransfersBetweenWarehouses(fromId: number, toId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/between/${fromId}/${toId}`, { withCredentials: true });
  }
}
