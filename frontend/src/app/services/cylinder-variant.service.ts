import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CylinderVariant } from '../models/cylinder-variant.model';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CylinderVariantService {
  private apiUrl = getApiUrl('/variants');

  constructor(private http: HttpClient) { }

  createVariant(variant: CylinderVariant): Observable<CylinderVariant> {
    return this.http.post<CylinderVariant>(this.apiUrl, variant);
  }

  getVariant(id: number): Observable<CylinderVariant> {
    return this.http.get<CylinderVariant>(`${this.apiUrl}/${id}`);
  }

  getAllVariants(page: number = 0, size: number = 20, sortBy: string = 'id', direction: string = 'ASC'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<any>(this.apiUrl, { params });
  }

  getActiveVariants(): Observable<CylinderVariant[]> {
    return this.http.get<CylinderVariant[]>(`${this.apiUrl}/active/list`);
  }

  updateVariant(id: number, variant: CylinderVariant): Observable<CylinderVariant> {
    return this.http.put<CylinderVariant>(`${this.apiUrl}/${id}`, variant);
  }

  deleteVariant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
