import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { BusinessInfo } from '../models/business-info.model';

@Injectable({ providedIn: 'root' })
export class BusinessInfoService {
  private apiUrl = getApiUrl('/business-info');

  constructor(private http: HttpClient) {}

  getBusinessInfoById(id: number | string): Observable<BusinessInfo> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BusinessInfo>());
  }

  createBusinessInfo(data: BusinessInfo): Observable<BusinessInfo> {
    return this.http.post<any>(this.apiUrl, data, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BusinessInfo>());
  }

  updateBusinessInfo(id: number | string, data: BusinessInfo): Observable<BusinessInfo> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, data, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<BusinessInfo>());
  }
}
