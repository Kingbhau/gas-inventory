import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Expense } from '../models/expense.model';
import { ExpenseCategoryService } from './expense-category.service';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';

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
    return this.http.post<Expense>(`${this.apiUrl}`, expense, { withCredentials: true })
      .pipe(applyTimeout());
  }

  // Get all expenses with pagination and filters
  getAllExpenses(page: number = 0, pageSize: number = 10, filters?: {
    fromDate?: string;
    toDate?: string;
    categoryId?: number;
    paymentMode?: string;
    bankAccountId?: number;
    minAmount?: number;
    maxAmount?: number;
  }): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    
    if (filters) {
      if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
      if (filters.toDate) params = params.set('toDate', filters.toDate);
      if (filters.categoryId) params = params.set('categoryId', filters.categoryId.toString());
      if (filters.paymentMode) params = params.set('paymentMode', filters.paymentMode);
      if (filters.bankAccountId) params = params.set('bankAccountId', filters.bankAccountId.toString());
      if (filters.minAmount !== undefined && filters.minAmount !== null) params = params.set('minAmount', filters.minAmount.toString());
      if (filters.maxAmount !== undefined && filters.maxAmount !== null) params = params.set('maxAmount', filters.maxAmount.toString());
    }
    
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout());
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

  // Get expenses summary (ALL matching records)
  getExpensesSummary(fromDate?: string, toDate?: string, categoryId?: number, paymentMode?: string, 
                     bankAccountId?: number, minAmount?: number, maxAmount?: number): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (categoryId) params = params.set('categoryId', categoryId.toString());
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    
    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(applyTimeout());
  }
}
