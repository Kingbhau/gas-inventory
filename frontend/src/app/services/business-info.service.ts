import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';


export interface BusinessInfo {
  agencyName: string;
  registrationNumber?: string;
  gstNumber?: string;
  address?: string;
  contactNumber?: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class BusinessInfoService {
  private apiUrl = getApiUrl('/business-info');

  constructor(private http: HttpClient) {}

  getBusinessInfoById(id: number | string): Observable<BusinessInfo> {
    return this.http.get<BusinessInfo>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  createBusinessInfo(data: BusinessInfo): Observable<BusinessInfo> {
    return this.http.post<BusinessInfo>(this.apiUrl, data, { withCredentials: true });
  }

  updateBusinessInfo(id: number | string, data: BusinessInfo): Observable<BusinessInfo> {
    return this.http.put<BusinessInfo>(`${this.apiUrl}/${id}`, data, { withCredentials: true });
  }
}
