import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPrint, faRefresh, faClipboard, faDownload } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { DayBookService } from '../../services/daybook.service';
import { LoadingService } from '../../services/loading.service';
import { SharedModule } from '../../shared/shared.module';
import { finalize } from 'rxjs';
import { exportDayBookReportToPDF } from './export-daybook-report.util';
import { UserService, User } from '../../services/user.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-daybook',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './daybook.component.html',
  styleUrl: './daybook.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayBookComponent implements OnInit, OnDestroy {
  // Font Awesome Icons
  faPrint = faPrint;
  faRefresh = faRefresh;
  faClipboard = faClipboard;
  faDownload = faDownload;

  // Data
  dayBookTransactions: any[] = [];
  dayBookSummary: any = null;
  selectedDate: string = '';
  isLoading = false;
  filterCreatedBy = '';
  filterTransactionType = '';
  users: User[] = [];
  transactionTypeOptions = [
    { value: '', label: 'All Types' },
    { value: 'SALE', label: 'Sale' },
    { value: 'EMPTY_RETURN', label: 'Empty Return' },
    { value: 'PAYMENT', label: 'Payment' },
    { value: 'TRANSFER', label: 'Ledger Transfer' },
    { value: 'WAREHOUSE_TRANSFER', label: 'Warehouse Transfer' },
    { value: 'SUPPLIER_TRANSACTION', label: 'Supplier Transaction' },
    { value: 'BANK_DEPOSIT', label: 'Bank Deposit' },
    { value: 'EXPENSE', label: 'Expense' }
  ];

  // Pagination
  dayBookPage = 1;
  dayBookPageSize = 10;
  dayBookTotalPages = 1;
  dayBookTotalElements = 0;
  daybookSortBy = 'transactionDate';
  daybookDirection = 'DESC';
  paginatedDayBookTransactions: any[] = [];

  constructor(
    private dayBookService: DayBookService,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {
    // Set default date to today
    this.selectedDate = this.getTodayDate();
  }

  ngOnInit() {
    this.loadUsers();
    this.loadDayBookData();
  }

  ngOnDestroy() {
    // Cleanup
  }

  /**
   * Get today's date in YYYY-MM-DD format
   */
  getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  /**
   * Load day book transactions for current/selected date
   */
  loadDayBookData() {
    this.isLoading = true;
    this.loadingService.show('Loading Day Book...');
    
    this.dayBookService.getTransactionsByDate(
      this.selectedDate, 
      this.dayBookPage - 1, 
      this.dayBookPageSize,
      this.daybookSortBy,
      this.daybookDirection,
      this.filterCreatedBy || undefined,
      this.filterTransactionType || undefined
    )
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.loadingService.hide();
        })
      )
      .subscribe({
        next: (response: any) => {
          // Response is a Page object with content, totalElements, totalPages, etc.
          this.paginatedDayBookTransactions = response.content || [];
          this.dayBookTotalElements = response.totalElements || 0;
          this.dayBookTotalPages = response.totalPages || 1;
          
          // Also fetch summary
          this.dayBookService.getTransactionsSummary(
            this.selectedDate,
            this.filterCreatedBy || undefined,
            this.filterTransactionType || undefined
          )
            .subscribe({
              next: (summaryResponse: any) => {
                this.dayBookSummary = summaryResponse;
                this.dayBookTransactions = summaryResponse.transactions || [];
                this.cdr.markForCheck();
              },
              error: (error: any) => {
                console.error('Error loading day book summary:', error);
              }
            });
          
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading day book data:', error);
          this.toastr.error('Failed to load day book data', 'Error');
          this.paginatedDayBookTransactions = [];
          this.dayBookSummary = null;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Refresh data
   */
  refreshData() {
    this.loadDayBookData();
  }

  /**
   * Change date and reload
   */
  onDateChange() {
    this.loadDayBookData();
  }

  /**
   * Export Day Book to PDF
   */
  printDayBook() {
    if (this.dayBookTransactions.length === 0) {
      this.toastr.error('No data to export', 'Error');
      return;
    }

    try {
      exportDayBookReportToPDF({
        transactions: this.dayBookTransactions,
        summary: this.dayBookSummary,
        selectedDate: this.selectedDate,
        businessName: 'GAS AGENCY SYSTEM'
      });
      this.toastr.success('PDF exported successfully!', 'Success');
    } catch (error) {
      console.error('Error generating PDF:', error);
      this.toastr.error('Failed to generate PDF', 'Error');
    }
  }

  /**
   * Handle page change
   */
  onDayBookPageChange(page: number) {
    this.dayBookPage = page;
    this.loadDayBookData();
  }

  /**
   * Handle page size change
   */
  onDayBookPageSizeChange(size: number) {
    this.dayBookPageSize = size;
    this.dayBookPage = 1;
    this.loadDayBookData();
  }

  /**
   * Format amount with proper decimal places
   */
  formatAmount(amount: any): string {
    if (!amount) return '0.00';
    const num = parseFloat(amount);
    if (isNaN(num)) return '0.00';
    return num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  getTypeLabel(type?: string | null): string {
    if (!type) return 'Transaction';
    switch (type) {
      case 'SALE':
        return 'Sale';
      case 'EMPTY_RETURN':
        return 'Empty Return';
      case 'PAYMENT':
        return 'Payment';
      case 'TRANSFER':
        return 'Ledger Transfer';
      case 'WAREHOUSE_TRANSFER':
        return 'Warehouse Transfer';
      case 'SUPPLIER_TRANSACTION':
        return 'Supplier Transaction';
      case 'BANK_DEPOSIT':
        return 'Bank Deposit';
      case 'EXPENSE':
        return 'Expense';
      default:
        return type.replace(/_/g, ' ');
    }
  }

  getCreatedByName(createdBy?: string | null): string {
    if (!createdBy) {
      return 'N/A';
    }
    const user = this.users.find(u => u.username === createdBy);
    return user?.name || createdBy;
  }

  private loadUsers() {
    this.userService.getUsers()
      .subscribe({
        next: (users) => {
          this.users = users || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.users = [];
        }
      });
  }
}
