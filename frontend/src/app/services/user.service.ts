import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

export interface User {
  id?: number;
  name: string;
  username?: string;
  mobileNo: string;
  role: string;
  active: boolean;
  businessId?: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = getApiUrl('/users');


  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl, { withCredentials: true })
      .pipe(applyTimeout());
  }

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  addUser(user: Partial<User>): Observable<User> {
    return this.http.post<User>(this.apiUrl, user, { withCredentials: true })
      .pipe(applyTimeout());
  }

  updateUser(id: number, user: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, user, { withCredentials: true });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  changePassword(id: number, currentPassword: string, newPassword: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${id}/change-password`, {
      currentPassword,
      newPassword
    }, { withCredentials: true });
  }
}
