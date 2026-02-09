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
import { AuthService } from '../../services/auth.service';
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
  dayBookTypeSummaries: Array<{
    type: string;
    label: string;
    count: number;
    totalAmount: number;
    amountReceived: number;
    dueAmount: number;
    quantityMoved: number;
  }> = [];
  selectedDate: string = '';
  isLoading = false;
  filterCreatedBy = '';
  filterTransactionType = '';
  users: User[] = [];
  currentUserRole: string | null = null;
  transactionTypeOptions = [
    { value: '', label: 'All Types' },
    { value: 'SALE', label: 'Sale' },
    { value: 'EMPTY_RETURN', label: 'Empty Return' },
    { value: 'PAYMENT', label: 'Payment' },
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
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    // Set default date to today
    this.selectedDate = this.getTodayDate();
  }

  ngOnInit() {
    const userInfo = this.authService.getUserInfo();
    this.currentUserRole = userInfo?.role || null;
    if (this.isManager) {
      this.transactionTypeOptions = this.transactionTypeOptions.filter(option => option.value !== 'BANK_DEPOSIT');
    }
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

    if (this.isManager && this.filterTransactionType === 'BANK_DEPOSIT') {
      this.filterTransactionType = '';
    }
    
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
          if (this.isManager) {
            const filteredPage = this.filterOutBankDeposits(response.content || []);
            this.paginatedDayBookTransactions = filteredPage;
            this.dayBookTotalElements = filteredPage.length;
            this.dayBookTotalPages = Math.max(1, Math.ceil(this.dayBookTotalElements / this.dayBookPageSize));
          } else {
            this.paginatedDayBookTransactions = response.content || [];
            this.dayBookTotalElements = response.totalElements || 0;
            this.dayBookTotalPages = response.totalPages || 1;
          }
          
          // Also fetch summary
          this.dayBookService.getTransactionsSummary(
            this.selectedDate,
            this.filterCreatedBy || undefined,
            this.filterTransactionType || undefined
          )
            .subscribe({
              next: (summaryResponse: any) => {
                if (this.isManager) {
                  const filteredTransactions = this.filterOutBankDeposits(summaryResponse?.transactions || []);
                  this.dayBookTransactions = filteredTransactions;
                this.dayBookSummary = this.buildSummaryFromTransactions(filteredTransactions);
                this.dayBookTypeSummaries = this.buildTypeSummaries(filteredTransactions);
                  this.applyPaginationFromTransactions(filteredTransactions);
                } else {
                  this.dayBookSummary = summaryResponse;
                  this.dayBookTransactions = summaryResponse.transactions || [];
                this.dayBookTypeSummaries = this.buildTypeSummaries(this.dayBookTransactions);
                }
                this.cdr.markForCheck();
              },
              error: (error: any) => {
                console.error('Error loading day book summary:', error);
                if (this.isManager) {
                  this.dayBookSummary = this.buildSummaryFromTransactions(this.paginatedDayBookTransactions);
                this.dayBookTypeSummaries = this.buildTypeSummaries(this.paginatedDayBookTransactions);
                }
              }
            });
          
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading day book data:', error);
          this.toastr.error('Failed to load day book data', 'Error');
          this.paginatedDayBookTransactions = [];
          this.dayBookSummary = null;
          this.dayBookTypeSummaries = [];
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

  get isManager(): boolean {
    return this.currentUserRole === 'MANAGER';
  }

  private filterOutBankDeposits(transactions: any[]): any[] {
    return (transactions || []).filter(tx => tx?.transactionType !== 'BANK_DEPOSIT');
  }

  private applyPaginationFromTransactions(transactions: any[]): void {
    const total = transactions.length;
    this.dayBookTotalElements = total;
    this.dayBookTotalPages = Math.max(1, Math.ceil(total / this.dayBookPageSize));
    if (this.dayBookPage > this.dayBookTotalPages) {
      this.dayBookPage = this.dayBookTotalPages;
    }
    const start = (this.dayBookPage - 1) * this.dayBookPageSize;
    this.paginatedDayBookTransactions = transactions.slice(start, start + this.dayBookPageSize);
  }

  private buildSummaryFromTransactions(transactions: any[]): any {
    const totalFilledCount = transactions.reduce((sum, tx) => sum + (Number(tx?.filledCount) || 0), 0);
    const totalEmptyCount = transactions.reduce((sum, tx) => sum + (Number(tx?.emptyCount) || 0), 0);
    const totalAmount = transactions.reduce((sum, tx) => sum + (Number(tx?.totalAmount) || 0), 0);
    const totalAmountReceived = transactions.reduce((sum, tx) => sum + (Number(tx?.amountReceived) || 0), 0);

    const latestPerCustomer = new Map<number, any>();
    transactions.forEach(tx => {
      const customerId = tx?.customerId;
      if (customerId != null && !latestPerCustomer.has(customerId)) {
        latestPerCustomer.set(customerId, tx);
      }
    });
    const totalDueAmount = Array.from(latestPerCustomer.values())
      .reduce((sum, tx) => sum + (Number(tx?.dueAmount) || 0), 0);

    return {
      transactions,
      totalFilledCount,
      totalEmptyCount,
      totalAmount,
      totalAmountReceived,
      totalDueAmount,
      totalTransactions: transactions.length
    };
  }

  private buildTypeSummaries(transactions: any[]): Array<{
    type: string;
    label: string;
    count: number;
    totalAmount: number;
    amountReceived: number;
    dueAmount: number;
    quantityMoved: number;
  }> {
    const types = this.transactionTypeOptions
      .filter(option => option.value)
      .map(option => option.value);

    const summaryMap = new Map<string, {
      type: string;
      label: string;
      count: number;
      totalAmount: number;
      amountReceived: number;
      dueAmount: number;
      quantityMoved: number;
    }>();

    types.forEach(type => {
      summaryMap.set(type, {
        type,
        label: this.getTypeLabel(type),
        count: 0,
        totalAmount: 0,
        amountReceived: 0,
        dueAmount: 0,
        quantityMoved: 0
      });
    });

    let totalReceivedAll = 0;
    let receivedTxnCount = 0;

    (transactions || []).forEach(tx => {
      const type = tx?.transactionType;
      if (!type || !summaryMap.has(type)) {
        return;
      }
      const summary = summaryMap.get(type);
      if (!summary) {
        return;
      }
      summary.count += 1;
      summary.totalAmount += Number(tx?.totalAmount) || 0;
      const received = Number(tx?.amountReceived) || 0;
      summary.amountReceived += received;
      summary.dueAmount += Number(tx?.dueAmount) || 0;
      if (type === 'WAREHOUSE_TRANSFER') {
        summary.quantityMoved += this.getTransferQuantity(tx?.details);
      }

      if (received > 0) {
        totalReceivedAll += received;
        receivedTxnCount += 1;
      }
    });

    const paymentSummary = summaryMap.get('PAYMENT');
    if (paymentSummary) {
      paymentSummary.amountReceived = totalReceivedAll;
      paymentSummary.count = receivedTxnCount;
    }

    return Array.from(summaryMap.values());
  }

  private getTransferQuantity(details?: string | null): number {
    if (!details) {
      return 0;
    }
    const match = details.match(/Qty:\s*(\d+)/i);
    if (!match) {
      return 0;
    }
    const qty = Number(match[1]);
    return isNaN(qty) ? 0 : qty;
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
