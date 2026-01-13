import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faDownload, faCalendarAlt } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { ExpenseService } from '../../services/expense.service';
import { LoadingService } from '../../services/loading.service';
import { Expense } from '../../models/expense.model';
import { exportExpenseReportToPDF } from '../reports/export-expense-report.util';
import { finalize } from 'rxjs/operators';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-expense-report',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './expense-report.component.html',
  styleUrl: './expense-report.component.css'
})
export class ExpenseReportComponent implements OnInit {
  @Input() agencyName: string = '';

  expenses: Expense[] = [];
  filteredExpenses: Expense[] = [];
  
  // Filter properties
  filterFromDate = '';
  filterToDate = '';
  filterCategory = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalExpenses = 0;
  totalPages = 1;

  categories: string[] = [];
  isLoading = false;
  private filtersApplied = false;

  // Icons
  faDownload = faDownload;
  faCalendarAlt = faCalendarAlt;

  // Summary data
  totalAmount = 0;
  transactionCount = 0;
  avgExpenseValue = 0;
  topCategory = 'N/A';

  constructor(
    private expenseService: ExpenseService,
    private loadingService: LoadingService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.loadCategories();
    this.loadExpenses();
  }

  loadCategories() {
    this.expenseService.getCategories().subscribe({
      next: (response: any) => {
        const categoryList = Array.isArray(response) ? response : (response.content || []);
        this.categories = categoryList.map((cat: any) => cat.name || cat);
      },
      error: () => {
        this.categories = ['Salary', 'Utilities', 'Maintenance', 'Transportation', 'Office Supplies', 'Other'];
      }
    });
  }

  loadExpenses(page: number = 1, pageSize: number = 10) {
    this.loadingService.show('Loading expenses...');
    this.isLoading = true;
    
    if (this.filterFromDate && this.filterToDate) {
      this.expenseService.getExpensesByDateRange(
        this.filterFromDate,
        this.filterToDate,
        page - 1,
        pageSize
      ).pipe(
        finalize(() => {
          this.isLoading = false;
          this.loadingService.hide();
        })
      ).subscribe({
        next: (response) => {
          this.expenses = response.content || response;
          this.totalExpenses = response.totalElements || response.length;
          this.totalPages = response.totalPages || Math.ceil(this.totalExpenses / pageSize);
          this.applyClientFilters();
          this.calculateSummary();
        },
        error: (error) => {
          this.toastr.error('Failed to load expenses');
          this.isLoading = false;
        }
      });
    } else {
      this.expenseService.getAllExpenses(page - 1, pageSize).pipe(
        finalize(() => {
          this.isLoading = false;
          this.loadingService.hide();
        })
      ).subscribe({
        next: (response) => {
          this.expenses = response.content || response;
          this.totalExpenses = response.totalElements || response.length;
          this.totalPages = response.totalPages || Math.ceil(this.totalExpenses / pageSize);
          this.applyClientFilters();
          this.calculateSummary();
        },
        error: (error) => {
          this.toastr.error('Failed to load expenses');
        }
      });
    }
  }

  applyClientFilters() {
    let filtered = [...this.expenses];

    // Category filter
    if (this.filterCategory) {
      filtered = filtered.filter(e => e.category === this.filterCategory);
    }

    // Amount range filter
    if (this.filterMinAmount !== null && typeof this.filterMinAmount === 'number') {
      filtered = filtered.filter(e => e.amount >= this.filterMinAmount!);
    }

    if (this.filterMaxAmount !== null && typeof this.filterMaxAmount === 'number') {
      filtered = filtered.filter(e => e.amount <= this.filterMaxAmount!);
    }

    this.filteredExpenses = filtered;
  }

  calculateSummary() {
    this.totalAmount = this.filteredExpenses.reduce((sum, expense) => sum + expense.amount, 0);
    this.transactionCount = this.filteredExpenses.length;
    this.avgExpenseValue = this.transactionCount > 0 ? this.totalAmount / this.transactionCount : 0;

    // Find top category by total amount
    const categorySums: { [key: string]: number } = {};
    this.filteredExpenses.forEach(expense => {
      const categoryName = expense.category || 'Uncategorized';
      categorySums[categoryName] = (categorySums[categoryName] || 0) + expense.amount;
    });

    if (Object.keys(categorySums).length > 0) {
      this.topCategory = Object.keys(categorySums).reduce((a, b) =>
        categorySums[a] > categorySums[b] ? a : b
      );
    }
  }

  applyFilters() {
    this.filtersApplied = true;
    this.currentPage = 1;
    this.loadExpenses(this.currentPage, this.pageSize);
  }

  resetFilters() {
    this.filterCategory = '';
    this.filterMinAmount = null;
    this.filterMaxAmount = null;
    this.currentPage = 1;
    
    // Reset dates to default (last 30 days)
    const today = new Date();
    const thirtyDaysAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
    
    this.filterToDate = today.toISOString().split('T')[0];
    this.filterFromDate = thirtyDaysAgo.toISOString().split('T')[0];
    
    this.filtersApplied = false;
    this.loadExpenses(this.currentPage, this.pageSize);
  }

  onPageChange(newPage: number) {
    if (newPage >= 1 && newPage <= this.totalPages) {
      this.currentPage = newPage;
      this.loadExpenses(this.currentPage, this.pageSize);
    }
  }

  onPageSizeChange(newSize: number) {
    this.pageSize = newSize;
    this.currentPage = 1;
    this.loadExpenses(this.currentPage, this.pageSize);
  }

  onCategorySelect(event: any): void {
    this.filterCategory = typeof event === 'string' ? event : '';
  }

  exportPDF() {
    if (this.filteredExpenses.length === 0) {
      this.toastr.warning('No data to export');
      return;
    }

    exportExpenseReportToPDF({
      expenseData: this.filteredExpenses,
      fromDate: this.filterFromDate,
      toDate: this.filterToDate,
      totalExpenseAmount: this.totalAmount,
      avgExpenseValue: this.avgExpenseValue,
      topCategory: this.topCategory,
      businessName: this.agencyName,
      minAmount: this.filterMinAmount || undefined,
      maxAmount: this.filterMaxAmount || undefined,
      transactionCount: this.transactionCount,
      categoryFilter: this.filterCategory || undefined
    });
  }
}
