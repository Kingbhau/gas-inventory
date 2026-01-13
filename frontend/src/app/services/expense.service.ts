import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Expense } from '../models/expense.model';
import { ExpenseCategoryService } from './expense-category.service';
import { getApiUrl } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  private apiUrl = getApiUrl('/expenses');

  constructor(
    private http: HttpClient,
    private categoryService: ExpenseCategoryService
  ) {}

  // Create a new expense
  createExpense(expense: Expense): Observable<Expense> {
    return this.http.post<Expense>(`${this.apiUrl}`, expense, { withCredentials: true });
  }

  // Get all expenses with pagination
  getAllExpenses(page: number = 0, pageSize: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true });
  }

  // Get expense by ID
  getExpenseById(id: number): Observable<Expense> {
    return this.http.get<Expense>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  // Get expenses by date range
  getExpensesByDateRange(fromDate: string, toDate: string, page: number = 0, pageSize: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate)
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}/range`, { params, withCredentials: true });
  }

  // Get expenses by category
  getExpensesByCategory(category: string, page: number = 0, pageSize: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('category', category)
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}/category`, { params, withCredentials: true });
  }

  // Update expense
  updateExpense(id: number, expense: Expense): Observable<Expense> {
    return this.http.put<Expense>(`${this.apiUrl}/${id}`, expense, { withCredentials: true });
  }

  // Delete expense
  deleteExpense(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  // Get expense categories with IDs
  getCategories(): Observable<any[]> {
    return this.categoryService.getAllCategories(0, 100);
  }
}
