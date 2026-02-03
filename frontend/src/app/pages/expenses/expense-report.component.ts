import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faDownload, faCalendarAlt, faEye } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { ExpenseService } from '../../services/expense.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { BankAccountService } from '../../services/bank-account.service';
import { LoadingService } from '../../services/loading.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { Expense } from '../../models/expense.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { BankAccount } from '../../models/bank-account.model';
import { exportExpenseReportToPDF } from '../reports/export-expense-report.util';
import { finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-expense-report',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './expense-report.component.html',
  styleUrl: './expense-report.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseReportComponent implements OnInit, OnDestroy {
  @Input() agencyName: string = '';

  expenses: Expense[] = [];
  filteredExpenses: Expense[] = [];
  selectedExpense: Expense | null = null;
  
  // Filter properties
  filterFromDate = '';
  filterToDate = '';
  filterCategory = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;
  filterPaymentMode = '';
  filterBankAccountId: string | number | null = null;

  // Filter data
  categories: string[] = [];
  categoryMap: Map<string, number> = new Map(); // Map category name to ID
  paymentModes: PaymentMode[] = [];
  bankAccounts: BankAccount[] = [];

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalExpenses = 0;
  totalPages = 1;
  isLoading = false;
  private filtersApplied = false;

  // Icons
  faDownload = faDownload;
  faCalendarAlt = faCalendarAlt;
  faEye = faEye;

  // Summary data
  totalAmount = 0;
  transactionCount = 0;
  avgExpenseValue = 0;
  topCategory = 'N/A';

  constructor(
    private expenseService: ExpenseService,
    private paymentModeService: PaymentModeService,
    private bankAccountService: BankAccountService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    private dateUtility: DateUtilityService
  ) {}

  ngOnInit() {
    this.loadFilterData();
    this.loadExpenses();
  }

  private loadFilterData() {
    forkJoin([
      this.expenseService.getCategories(),
      this.paymentModeService.getActivePaymentModes(),
      this.bankAccountService.getActiveBankAccounts()
    ])
    .pipe(finalize(() => {}))
    .subscribe({
      next: ([categoryResponse, paymentModes, bankAccounts]: any) => {
        const categoryList = Array.isArray(categoryResponse) ? categoryResponse : (categoryResponse?.content || []);
        this.categories = categoryList.map((cat: any) => cat.name || cat);
        // Build category map
        categoryList.forEach((cat: any) => {
          this.categoryMap.set(cat.name, cat.id);
        });
        this.paymentModes = paymentModes || [];
        this.bankAccounts = bankAccounts || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.categories = ['Salary', 'Utilities', 'Maintenance', 'Transportation', 'Office Supplies', 'Other'];
        this.cdr.markForCheck();
      }
    });
  }

  loadExpenses(page: number = 1, pageSize: number = 10) {
    this.loadingService.show('Loading expenses...');
    this.isLoading = true;
    
    // Build filters object
    const filters: any = {};
    if (this.filterFromDate) filters.fromDate = this.filterFromDate;
    if (this.filterToDate) filters.toDate = this.filterToDate;
    if (this.filterCategory) filters.categoryId = this.getCategoryId(this.filterCategory);
    if (this.filterPaymentMode) filters.paymentMode = this.filterPaymentMode;
    if (this.filterBankAccountId) filters.bankAccountId = Number(this.filterBankAccountId);
    if (this.filterMinAmount !== null && this.filterMinAmount !== undefined) filters.minAmount = this.filterMinAmount;
    if (this.filterMaxAmount !== null && this.filterMaxAmount !== undefined) filters.maxAmount = this.filterMaxAmount;

    this.expenseService.getAllExpenses(page - 1, pageSize, filters).pipe(
      finalize(() => {
        this.isLoading = false;
        this.loadingService.hide();
      })
    ).subscribe({
      next: (response) => {
        this.expenses = response.content || response;
        this.totalExpenses = response.totalElements || response.length;
        this.totalPages = response.totalPages || Math.ceil(this.totalExpenses / pageSize);
        this.filteredExpenses = this.expenses; // Backend already filtered
        this.loadSummary();
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.toastr.error('Failed to load expenses');
        this.isLoading = false;
      }
    });
  }

  loadSummary() {
    this.expenseService.getExpensesSummary(
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      undefined,
      this.filterPaymentMode || undefined,
      this.filterBankAccountId ? Number(this.filterBankAccountId) : undefined,
      this.filterMinAmount || undefined,
      this.filterMaxAmount || undefined
    ).subscribe({
      next: (summary: any) => {
        this.totalAmount = summary.totalAmount || 0;
        this.transactionCount = summary.transactionCount || 0;
        this.avgExpenseValue = summary.avgExpenseValue || 0;
        this.topCategory = summary.topCategory || 'N/A';
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load summary', error);
        // Fallback to empty values
        this.totalAmount = 0;
        this.transactionCount = 0;
        this.avgExpenseValue = 0;
        this.topCategory = 'N/A';
      }
    });
  }

  applyClientFilters() {
    // Sorting by date (already handled by backend with proper sorting)
    this.filteredExpenses = this.expenses.sort((a, b) => {
      const dateA = new Date(a.expenseDate).getTime();
      const dateB = new Date(b.expenseDate).getTime();
      if (dateB !== dateA) {
        return dateB - dateA;
      }
      return (b.id || 0) - (a.id || 0);
    });
    this.cdr.markForCheck();
  }

  calculateSummary() {
    // No longer used - summary now loaded from backend via loadSummary()
    this.cdr.markForCheck();
  }

  ngOnDestroy() {}

  applyFilters() {
    this.filtersApplied = true;
    this.currentPage = 1;
    this.loadExpenses(this.currentPage, this.pageSize);
    this.cdr.markForCheck();
  }

  resetFilters() {
    this.filterCategory = '';
    this.filterMinAmount = null;
    this.filterMaxAmount = null;
    this.filterPaymentMode = '';
    this.filterBankAccountId = null;
    this.currentPage = 1;
    
    // Reset dates to default (last 30 days) using IST
    const today = new Date();
    const thirtyDaysAgo = this.dateUtility.addDays(today, -30);
    
    this.filterToDate = this.dateUtility.getLocalDateString(today);
    this.filterFromDate = this.dateUtility.getLocalDateString(thirtyDaysAgo);
    
    this.filtersApplied = false;
    this.loadExpenses(this.currentPage, this.pageSize);
    this.cdr.markForCheck();
  }

  onPageChange(newPage: number) {
    if (newPage >= 1 && newPage <= this.totalPages) {
      this.currentPage = newPage;
      this.loadExpenses(this.currentPage, this.pageSize);
      this.cdr.markForCheck();
    }
  }

  onPageSizeChange(newSize: number) {
    this.pageSize = newSize;
    this.currentPage = 1;
    this.loadExpenses(this.currentPage, this.pageSize);
    this.cdr.markForCheck();
  }

  onCategorySelect(event: any): void {
    this.filterCategory = typeof event === 'string' ? event : '';
    this.cdr.markForCheck();
  }

  exportPDF() {
    if (this.filteredExpenses.length === 0) {
      this.toastr.warning('No data to export');
      return;
    }

    const selectedBank = this.filterBankAccountId ? 
      this.bankAccounts.find(b => b.id === Number(this.filterBankAccountId)) : undefined;
    const bankDetails = selectedBank ? 
      `${selectedBank.bankName} - ${selectedBank.accountNumber}` : undefined;

    // Enrich expense data with bank names for each expense
    const enrichedExpenses = this.filteredExpenses.map((expense: any) => ({
      ...expense,
      bankDetails: expense.bankAccountId ? 
        (() => {
          const bank = this.bankAccounts.find(b => b.id === expense.bankAccountId);
          return bank ? `${bank.bankName} -\n${bank.accountNumber}` : expense.bankAccountNumber || '-';
        })() : '-'
    }));

    exportExpenseReportToPDF({
      expenseData: enrichedExpenses,
      fromDate: this.filterFromDate,
      toDate: this.filterToDate,
      totalExpenseAmount: this.totalAmount,
      avgExpenseValue: this.avgExpenseValue,
      topCategory: this.topCategory,
      businessName: this.agencyName,
      minAmount: this.filterMinAmount || undefined,
      maxAmount: this.filterMaxAmount || undefined,
      transactionCount: this.transactionCount,
      categoryFilter: this.filterCategory || undefined,
      paymentMode: this.filterPaymentMode || undefined,
      bankAccountName: bankDetails
    });
  }

  viewDetails(expense: Expense) {
    this.selectedExpense = expense;
  }

  closeDetails() {
    this.selectedExpense = null;
  }

  getSelectedPaymentMode(): PaymentMode | undefined {
    if (!this.filterPaymentMode) return undefined;
    return this.paymentModes.find(pm => pm.name === this.filterPaymentMode);
  }

  getCategoryId(categoryName: string): number | undefined {
    return this.categoryMap.get(categoryName);
  }

  /**
   * Get local date string in YYYY-MM-DD format without timezone conversion
   */
  /**
   * Get local date string in YYYY-MM-DD format without timezone conversion
   */
  private getLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
