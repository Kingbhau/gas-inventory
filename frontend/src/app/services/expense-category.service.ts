import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { expand, map, reduce } from 'rxjs';
import { ExpenseCategory } from '../models/expense-category.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

@Injectable({
  providedIn: 'root'
})
export class ExpenseCategoryService {
  private apiUrl =getApiUrl('/expense-categories');

  constructor(private http: HttpClient) {}

  // Get all categories
  getAllCategories(page: number = 0, pageSize: number = 100): Observable<PageResponse<ExpenseCategory>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<ExpenseCategory>>());
  }

  getAllCategoriesAll(pageSize: number = 200): Observable<ExpenseCategory[]> {
    return this.getAllCategories(0, pageSize).pipe(
      expand((response: PageResponse<ExpenseCategory>) => {
        const currentPage = response?.page ?? 0;
        const totalPages = response?.totalPages ?? 0;
        const nextPage = currentPage + 1;
        return nextPage < totalPages ? this.getAllCategories(nextPage, pageSize) : EMPTY;
      }),
      map((response: PageResponse<ExpenseCategory>) => response?.items ?? response ?? []),
      reduce((all: ExpenseCategory[], chunk: ExpenseCategory[]) => all.concat(chunk), [])
    );
  }

  // Get active categories only
  getActiveCategories(): Observable<ExpenseCategory[]> {
    return this.http.get<any>(`${this.apiUrl}/active`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseCategory[]>());
  }

  // Get category by ID
  getCategoryById(id: number): Observable<ExpenseCategory> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseCategory>());
  }

  // Create new category
  createCategory(category: ExpenseCategory): Observable<ExpenseCategory> {
    return this.http.post<any>(`${this.apiUrl}`, category, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseCategory>());
  }

  // Update category
  updateCategory(id: number, category: ExpenseCategory): Observable<ExpenseCategory> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, category, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseCategory>());
  }

  // Delete category
  deleteCategory(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  // Activate/Deactivate category
  toggleCategoryStatus(id: number, isActive: boolean): Observable<ExpenseCategory> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/status`, { isActive }, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseCategory>());
  }

  // Get category names only
  getCategoryNames(): Observable<string[]> {
    return this.http.get<any>(`${this.apiUrl}/names`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<string[]>());
  }
}
