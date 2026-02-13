
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';
import { AuthLoginResponse, AuthRegisterResponse, AuthUserInfo } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = getApiUrl('/auth');

  constructor(private http: HttpClient) {}

    // ...existing code...

  refreshToken(): Observable<void> {
    return this.http.post<any>(`${this.apiUrl}/refresh`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<void>());
  }

  login(username: string, password: string): Observable<AuthLoginResponse> {
    // withCredentials ensures cookie is set
    return this.http.post<any>(`${this.apiUrl}/login`, { username, password }, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<AuthLoginResponse>());
  }

  register(username: string, password: string, role: string): Observable<AuthRegisterResponse> {
    return this.http.post<any>(`${this.apiUrl}/register`, { username, password, role }, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<AuthRegisterResponse>());
  }


  // setToken removed: JWT is now stored in HTTP-only cookie


  // getToken removed: JWT is now stored in HTTP-only cookie


  logout(): Observable<void> {
    // Call backend to clear cookie
    return this.http.post<any>(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<void>());
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<any>(`${this.apiUrl}/forgot-password`, { email })
      .pipe(applyTimeout(), unwrapApiResponse<void>());
  }


  getLoggedInUserName(): string {
    const info = this.getUserInfo();
    return info?.name || '';
  }

  setUserInfo(user: AuthUserInfo) {
    // Store only non-sensitive user info in localStorage (no token)
    const userInfo = {
      id: user.id,
      username: user.username,
      name: user.name,
      role: user.role,
      mobileNo: user.mobileNo,
      businessId: user.businessId ?? null
    };
    localStorage.setItem('user_info', JSON.stringify(userInfo));
    if (user.role) {
      localStorage.setItem('user_role', user.role);
    }
    if (user.name) {
      localStorage.setItem('user_name', user.name);
    }
  }

  getUserInfo(): AuthUserInfo | null {
    const info = localStorage.getItem('user_info');
    return info ? JSON.parse(info) : null;
  }
}
