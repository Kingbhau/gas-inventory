import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { CacheService, CACHE_KEYS } from './cache.service';
import { unwrapApiResponse } from '../utils/api-response.util';
import { SimpleStatusDTO } from '../models/simple-status';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = getApiUrl('/users');

  constructor(private http: HttpClient, private cacheService: CacheService) {}

  getUsers(): Observable<User[]> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<User[]>());
  }

  getUser(id: number): Observable<User> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<User>());
  }

  invalidateCache(): void {
    this.cacheService.invalidate(CACHE_KEYS.USERS);
  }

  addUser(user: Partial<User>): Observable<User> {
    return this.http.post<any>(this.apiUrl, user, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<User>());
  }

  updateUser(id: number, user: Partial<User>): Observable<User> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, user, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<User>());
  }

  deleteUser(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  changePassword(id: number, currentPassword: string, newPassword: string): Observable<SimpleStatusDTO> {
    return this.http.post<any>(`${this.apiUrl}/${id}/change-password`, {
      currentPassword,
      newPassword
    }, { withCredentials: true }).pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  changeOwnPassword(currentPassword: string, newPassword: string): Observable<SimpleStatusDTO> {
    const staffUrl = getApiUrl('/staff/change-password');
    return this.http.post<any>(staffUrl, {
      currentPassword,
      newPassword
    }, { withCredentials: true }).pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  reactivateUser(id: number): Observable<User> {
    return this.http.post<any>(`${this.apiUrl}/${id}/reactivate`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<User>());
  }
}
