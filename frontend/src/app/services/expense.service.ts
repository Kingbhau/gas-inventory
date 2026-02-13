import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Expense } from '../models/expense.model';
import { ExpenseCategory } from '../models/expense-category.model';
import { ExpenseFilters } from '../models/expense-filters.model';
import { ExpenseSummary } from '../models/expense-summary.model';
import { PageResponse } from '../models/page-response';
import { SimpleStatusDTO } from '../models/simple-status';
import { ExpenseCategoryService } from './expense-category.service';
import { getApiUrl } from '../config/api.config';
import { applyTimeout } from '../config/http.config';
import { unwrapApiResponse } from '../utils/api-response.util';

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
    return this.http.post<any>(`${this.apiUrl}`, expense, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Expense>());
  }

  // Get all expenses with pagination and filters
  getAllExpenses(page: number = 0, pageSize: number = 10, filters?: ExpenseFilters): Observable<PageResponse<Expense>> {
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
      if (filters.createdBy) params = params.set('createdBy', filters.createdBy);
    }
    
    return this.http.get<any>(`${this.apiUrl}`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Expense>>());
  }

  // Get expense by ID
  getExpenseById(id: number): Observable<Expense> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Expense>());
  }

  // Get expenses by date range
  getExpensesByDateRange(fromDate: string, toDate: string, page: number = 0, pageSize: number = 10): Observable<PageResponse<Expense>> {
    const params = new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate)
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}/range`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Expense>>());
  }

  // Get expenses by category
  getExpensesByCategory(category: string, page: number = 0, pageSize: number = 10): Observable<PageResponse<Expense>> {
    const params = new HttpParams()
      .set('category', category)
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.http.get<any>(`${this.apiUrl}/category`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<PageResponse<Expense>>());
  }

  // Update expense
  updateExpense(id: number, expense: Expense): Observable<Expense> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, expense, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<Expense>());
  }

  // Delete expense
  deleteExpense(id: number): Observable<SimpleStatusDTO> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<SimpleStatusDTO>());
  }

  // Get expense categories with IDs
  getCategories(): Observable<ExpenseCategory[]> {
    return this.categoryService.getAllCategoriesAll();
  }

  // Get expenses summary (ALL matching records)
  getExpensesSummary(fromDate?: string, toDate?: string, categoryId?: number, paymentMode?: string, 
                     bankAccountId?: number, minAmount?: number, maxAmount?: number, createdBy?: string): Observable<ExpenseSummary> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (categoryId) params = params.set('categoryId', categoryId.toString());
    if (paymentMode) params = params.set('paymentMode', paymentMode);
    if (bankAccountId) params = params.set('bankAccountId', bankAccountId.toString());
    if (minAmount !== undefined && minAmount !== null) params = params.set('minAmount', minAmount.toString());
    if (maxAmount !== undefined && maxAmount !== null) params = params.set('maxAmount', maxAmount.toString());
    if (createdBy) params = params.set('createdBy', createdBy);
    
    return this.http.get<any>(`${this.apiUrl}/summary`, { params, withCredentials: true })
      .pipe(applyTimeout(), unwrapApiResponse<ExpenseSummary>());
  }
}
