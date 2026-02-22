import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { getApiUrl } from '../config/api.config';
import { PageResponse } from '../models/page-response';
import { AuditRecord } from '../models/audit-record.model';

@Injectable({
  providedIn: 'root'
})
export class AuditRecordService {
  private apiUrl = getApiUrl('/audit-records');

  constructor(private http: HttpClient) {}

  getAuditRecords(
    entityType: string,
    entityId: number,
    fieldName?: string,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<AuditRecord>> {
    let params = new HttpParams()
      .set('entityType', entityType)
      .set('entityId', entityId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    if (fieldName && fieldName.trim()) {
      params = params.set('fieldName', fieldName.trim());
    }
    return this.http.get<any>(this.apiUrl, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<AuditRecord>>());
  }
}
