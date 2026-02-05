import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { ExpenseCategory } from '../models/expense-category.model';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

@Injectable({
  providedIn: 'root'
})
export class ExpenseCategoryService {
  private apiUrl =getApiUrl('/expense-categories');

  constructor(private http: HttpClient) {}

  // Get all categories
  getAllCategories(page: number = 0, pageSize: number = 100): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }

  getAllCategoriesAll(pageSize: number = 200): Observable<ExpenseCategory[]> {
    return this.getAllCategories(0, pageSize).pipe(
      expand((response: any) => {
        const currentPage = response?.number ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllCategories(nextPage, pageSize) : EMPTY;
      }),
      map((response: any) => response?.content ?? response ?? []),
      reduce((all: ExpenseCategory[], chunk: ExpenseCategory[]) => all.concat(chunk), [])
    );
  }

  // Get active categories only
  getActiveCategories(): Observable<ExpenseCategory[]> {
    return this.http.get<ExpenseCategory[]>(`${this.apiUrl}/active`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Get category by ID
  getCategoryById(id: number): Observable<ExpenseCategory> {
    return this.http.get<ExpenseCategory>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Create new category
  createCategory(category: ExpenseCategory): Observable<ExpenseCategory> {
    return this.http.post<ExpenseCategory>(`${this.apiUrl}`, category, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Update category
  updateCategory(id: number, category: ExpenseCategory): Observable<ExpenseCategory> {
    return this.http.put<ExpenseCategory>(`${this.apiUrl}/${id}`, category, { withCredentials: true });
  }

  // Delete category
  deleteCategory(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  // Activate/Deactivate category
  toggleCategoryStatus(id: number, isActive: boolean): Observable<ExpenseCategory> {
    return this.http.patch<ExpenseCategory>(`${this.apiUrl}/${id}/status`, { isActive }, { withCredentials: true });
  }

  // Get category names only
  getCategoryNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/names`, { withCredentials: true });
  }
}
