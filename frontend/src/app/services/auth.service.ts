
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = getApiUrl('/auth');

  constructor(private http: HttpClient) {}

    // ...existing code...

  refreshToken(): Observable<any> {
    return this.http.post(`${this.apiUrl}/refresh`, {}, { withCredentials: true })
      .pipe(applyTimeout());
  }

  login(username: string, password: string): Observable<any> {
    // withCredentials ensures cookie is set
    return this.http.post(`${this.apiUrl}/login`, { username, password }, { withCredentials: true })
      .pipe(applyTimeout());
  }

  register(username: string, password: string, role: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, { username, password, role })
      .pipe(applyTimeout());
  }


  // setToken removed: JWT is now stored in HTTP-only cookie


  // getToken removed: JWT is now stored in HTTP-only cookie


  logout(): Observable<any> {
    // Call backend to clear cookie
    return this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(applyTimeout());
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email })
      .pipe(applyTimeout());
  }


  getLoggedInUserName(): string {
    const info = this.getUserInfo();
    return info?.name || '';
  }

  setUserInfo(user: any) {
    // Store only non-sensitive user info in localStorage (no token)
    const userInfo = {
      id: user.id,
      username: user.username,
      name: user.name,
      role: user.role,
      mobileNo: user.mobileNo,
      businessId: user.businessId || user.business_id || (user.business && user.business.id) || null
    };
    localStorage.setItem('user_info', JSON.stringify(userInfo));
    if (user.role) {
      localStorage.setItem('user_role', user.role);
    }
    if (user.name) {
      localStorage.setItem('user_name', user.name);
    }
  }

  getUserInfo(): any {
    const info = localStorage.getItem('user_info');
    return info ? JSON.parse(info) : null;
  }
}
