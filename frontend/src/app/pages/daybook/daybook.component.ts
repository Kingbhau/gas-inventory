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
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { DayBook, DayBookSummary } from '../../models/daybook.model';
import { PageResponse } from '../../models/page-response';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';

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
  dayBookTransactions: DayBook[] = [];
  dayBookSummary: DayBookSummary | null = null;
  dayBookTypeSummaries: Array<{
    type: string;
    label: string;
    count: number;
    totalAmount: number;
    amountReceived: number;
    dueAmount: number;
    quantityMoved: number;
  }> = [];
  variantSummaries: Array<{
    variantName: string;
    filledCount: number;
    emptyCount: number;
  }> = [];
  filledVariantSummaries: Array<{
    variantName: string;
    filledCount: number;
  }> = [];
  emptyVariantSummaries: Array<{
    variantName: string;
    emptyCount: number;
  }> = [];
  paymentModeBreakdown: Array<{
    mode: string;
    amount: number;
    count: number;
  }> = [];
  salePaymentModeBreakdown: Array<{
    mode: string;
    amount: number;
    count: number;
  }> = [];
  emptyReturnPaymentModeBreakdown: Array<{
    mode: string;
    amount: number;
    count: number;
  }> = [];
  activePaymentBreakdown: Array<{
    mode: string;
    amount: number;
    count: number;
  }> = [];
  paymentBreakdownTitle = 'Payment Mode Breakdown';
  showPaymentModeBreakdownModal = false;
  showVerificationModal = false;
  verificationAction: 'VERIFY' | 'PENDING' = 'VERIFY';
  verificationTarget: DayBook | null = null;
  verificationRemark = '';
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
  private readonly staffAllowedTransactionTypes = ['', 'SALE', 'EMPTY_RETURN', 'PAYMENT'];

  // Pagination
  dayBookPage = 1;
  dayBookPageSize = 10;
  dayBookTotalPages = 1;
  dayBookTotalElements = 0;
  daybookSortBy = 'transactionDate';
  daybookDirection = 'DESC';
  paginatedDayBookTransactions: DayBook[] = [];

  constructor(
    private dayBookService: DayBookService,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private userService: UserService,
    private authService: AuthService,
    private ledgerService: CustomerCylinderLedgerService,
    private cdr: ChangeDetectorRef
  ) {
    // Set default date to today
    this.selectedDate = this.getTodayDate();
  }

  ngOnInit() {
    const userInfo = this.authService.getUserInfo();
    this.currentUserRole = userInfo?.role || null;
    if (this.isStaff) {
      this.transactionTypeOptions = this.transactionTypeOptions
        .filter(option => this.staffAllowedTransactionTypes.includes(option.value));
      if (!this.staffAllowedTransactionTypes.includes(this.filterTransactionType)) {
        this.filterTransactionType = '';
      }
      this.filterCreatedBy = userInfo?.username || this.filterCreatedBy;
      this.users = [];
    } else {
      this.loadUsers();
    }
    if (this.isManager) {
      this.transactionTypeOptions = this.transactionTypeOptions.filter(option => option.value !== 'BANK_DEPOSIT');
    }
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
    if (this.isStaff && !this.staffAllowedTransactionTypes.includes(this.filterTransactionType)) {
      this.filterTransactionType = '';
    }
    
    this.dayBookService.getTransactionsByDate(
      this.selectedDate, 
      this.dayBookPage - 1, 
      this.dayBookPageSize,
      this.daybookSortBy,
      this.daybookDirection,
      (this.isStaff ? undefined : (this.filterCreatedBy || undefined)),
      this.filterTransactionType || undefined
    )
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.loadingService.hide();
        })
      )
      .subscribe({
        next: (response: PageResponse<DayBook>) => {
          // Response is a Page object with content, totalElements, totalPages, etc.
          if (this.isManager) {
            const filteredPage = this.filterOutBankDeposits(response.items || []);
            this.paginatedDayBookTransactions = filteredPage;
            this.dayBookTotalElements = filteredPage.length;
            this.dayBookTotalPages = Math.max(1, Math.ceil(this.dayBookTotalElements / this.dayBookPageSize));
          } else {
            this.paginatedDayBookTransactions = response.items || [];
            this.dayBookTotalElements = response.totalElements ?? 0;
            this.dayBookTotalPages = response.totalPages ?? 1;
          }
          
          // Also fetch summary
          this.dayBookService.getTransactionsSummary(
            this.selectedDate,
            (this.isStaff ? undefined : (this.filterCreatedBy || undefined)),
            this.filterTransactionType || undefined
          )
            .subscribe({
              next: (summaryResponse: DayBookSummary) => {
                if (this.isManager) {
                  const filteredTransactions = this.filterOutBankDeposits(summaryResponse?.transactions || []);
                  this.dayBookTransactions = filteredTransactions;
                  this.dayBookSummary = this.buildSummaryFromTransactions(filteredTransactions);
                  this.dayBookTypeSummaries = this.buildTypeSummaries(filteredTransactions);
                  this.setVariantSummaries(filteredTransactions);
                  this.setPaymentModeBreakdown(filteredTransactions);
                  this.setTypePaymentModeBreakdowns(filteredTransactions);
                  this.applyPaginationFromTransactions(filteredTransactions);
                } else {
                  this.dayBookSummary = summaryResponse;
                  this.dayBookTransactions = summaryResponse.transactions || [];
                  this.dayBookTypeSummaries = this.buildTypeSummaries(this.dayBookTransactions);
                  this.setVariantSummaries(this.dayBookTransactions);
                  this.setPaymentModeBreakdown(this.dayBookTransactions);
                  this.setTypePaymentModeBreakdowns(this.dayBookTransactions);
                }
                this.cdr.markForCheck();
              },
              error: (error: unknown) => {
                if (this.isManager) {
                  this.dayBookSummary = this.buildSummaryFromTransactions(this.paginatedDayBookTransactions);
                  this.dayBookTypeSummaries = this.buildTypeSummaries(this.paginatedDayBookTransactions);
                  this.setVariantSummaries(this.paginatedDayBookTransactions);
                  this.setPaymentModeBreakdown(this.paginatedDayBookTransactions);
                  this.setTypePaymentModeBreakdowns(this.paginatedDayBookTransactions);
                }
              }
            });
          
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.toastr.error('Failed to load day book data', 'Error');
          this.paginatedDayBookTransactions = [];
          this.dayBookSummary = null;
          this.dayBookTypeSummaries = [];
          this.variantSummaries = [];
          this.filledVariantSummaries = [];
          this.emptyVariantSummaries = [];
          this.paymentModeBreakdown = [];
          this.salePaymentModeBreakdown = [];
          this.emptyReturnPaymentModeBreakdown = [];
          this.activePaymentBreakdown = [];
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
      this.toastr.info('No data to export');
      return;
    }

    try {
      exportDayBookReportToPDF({
        transactions: this.dayBookTransactions,
        summary: this.dayBookSummary ?? this.buildSummaryFromTransactions(this.dayBookTransactions),
        typeSummaries: this.dayBookTypeSummaries,
        filledVariantSummaries: this.filledVariantSummaries,
        emptyVariantSummaries: this.emptyVariantSummaries,
        selectedDate: this.selectedDate,
        businessName: 'GAS AGENCY SYSTEM'
      });
      this.toastr.success('PDF exported successfully!', 'Success');
    } catch (error) {
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
  formatAmount(amount: string | number | null | undefined): string {
    if (amount === null || amount === undefined || amount === '') return '0.00';
    const num = typeof amount === 'number' ? amount : parseFloat(String(amount));
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
        return 'Payment Collection';
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

  getVerificationStatusLabel(transaction: DayBook): string {
    const amountReceived = Number(transaction?.amountReceived) || 0;
    if (amountReceived <= 0) {
      return '-';
    }
    return transaction?.verificationStatus || 'PENDING';
  }

  getVerificationStatusClass(transaction: DayBook): string {
    const status = this.getVerificationStatusLabel(transaction);
    if (status === 'VERIFIED') return 'status-verified';
    if (status === 'REJECTED') return 'status-rejected';
    if (status === 'PENDING') return 'status-pending';
    return 'status-na';
  }

  get isManager(): boolean {
    return this.currentUserRole === 'MANAGER';
  }

  get isStaff(): boolean {
    return this.currentUserRole === 'STAFF';
  }

  get isOwner(): boolean {
    return this.currentUserRole === 'OWNER';
  }

  isVerificationClickable(transaction: DayBook): boolean {
    return this.isOwner && this.getVerificationStatusLabel(transaction) !== '-' && !!transaction.id;
  }

  onVerificationBadgeClick(transaction: DayBook): void {
    if (!this.isVerificationClickable(transaction) || !transaction.id) {
      return;
    }
    this.verificationTarget = transaction;
    this.verificationAction = this.getVerificationStatusLabel(transaction) === 'PENDING' ? 'VERIFY' : 'PENDING';
    this.verificationRemark = '';
    this.showVerificationModal = true;
    this.cdr.markForCheck();
  }

  closeVerificationModal(): void {
    this.showVerificationModal = false;
    this.verificationTarget = null;
    this.verificationRemark = '';
    this.verificationAction = 'VERIFY';
    this.cdr.markForCheck();
  }

  confirmVerificationAction(): void {
    if (!this.verificationTarget?.id) {
      this.closeVerificationModal();
      return;
    }

    if (this.verificationAction === 'PENDING' && !this.verificationRemark.trim()) {
      this.toastr.info('Remark is required', 'Info');
      return;
    }

    if (this.verificationAction === 'VERIFY') {
      this.ledgerService.verifyBankConfirmation(this.verificationTarget.id, this.verificationRemark.trim() || undefined).subscribe({
        next: () => {
          this.toastr.success('Transaction verified', 'Success');
          this.closeVerificationModal();
          this.loadDayBookData();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          this.toastr.error(err?.error?.message || err?.message || 'Verification failed', 'Error');
        }
      });
      return;
    }

    this.ledgerService.markBankConfirmationPending(this.verificationTarget.id, this.verificationRemark.trim()).subscribe({
      next: () => {
        this.toastr.success('Transaction moved to pending', 'Success');
        this.closeVerificationModal();
        this.loadDayBookData();
      },
      error: (error: unknown) => {
        const err = error as { error?: { message?: string }; message?: string };
        this.toastr.error(err?.error?.message || err?.message || 'Update failed', 'Error');
      }
    });
  }

  rejectVerificationAction(): void {
    if (!this.verificationTarget?.id || this.verificationAction !== 'VERIFY') {
      return;
    }
    if (!this.verificationRemark.trim()) {
      this.toastr.info('Remark is required to reject a transaction', 'Info');
      return;
    }
    this.ledgerService.rejectBankConfirmation(this.verificationTarget.id, this.verificationRemark.trim() || undefined).subscribe({
      next: () => {
        this.toastr.success('Transaction rejected', 'Success');
        this.closeVerificationModal();
        this.loadDayBookData();
      },
      error: (error: unknown) => {
        const err = error as { error?: { message?: string }; message?: string };
        this.toastr.error(err?.error?.message || err?.message || 'Rejection failed', 'Error');
      }
    });
  }

  private filterOutBankDeposits(transactions: DayBook[]): DayBook[] {
    return (transactions || []).filter(tx => tx?.transactionType !== 'BANK_DEPOSIT');
  }

  private applyPaginationFromTransactions(transactions: DayBook[]): void {
    const total = transactions.length;
    this.dayBookTotalElements = total;
    this.dayBookTotalPages = Math.max(1, Math.ceil(total / this.dayBookPageSize));
    if (this.dayBookPage > this.dayBookTotalPages) {
      this.dayBookPage = this.dayBookTotalPages;
    }
    const start = (this.dayBookPage - 1) * this.dayBookPageSize;
    this.paginatedDayBookTransactions = transactions.slice(start, start + this.dayBookPageSize);
  }

  private buildSummaryFromTransactions(transactions: DayBook[]): DayBookSummary {
    const totalFilledCount = transactions.reduce((sum, tx) => sum + (Number(tx?.filledCount) || 0), 0);
    const totalEmptyCount = transactions.reduce((sum, tx) => sum + (Number(tx?.emptyCount) || 0), 0);
    const totalAmount = transactions.reduce((sum, tx) => sum + (Number(tx?.totalAmount) || 0), 0);
    const totalAmountReceived = transactions.reduce((sum, tx) => sum + (Number(tx?.amountReceived) || 0), 0);

    const latestPerCustomer = new Map<number, DayBook>();
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

  private buildTypeSummaries(transactions: DayBook[]): Array<{
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

  private buildVariantSummaries(transactions: DayBook[]): Array<{
    variantName: string;
    filledCount: number;
    emptyCount: number;
  }> {
    const summaryMap = new Map<string, { variantName: string; filledCount: number; emptyCount: number }>();

    (transactions || []).forEach(tx => {
      const rawName = typeof tx?.variantName === 'string' ? tx.variantName.trim() : '';
      const filledCount = Number(tx?.filledCount) || 0;
      const emptyCount = Number(tx?.emptyCount) || 0;
      if (!rawName && filledCount === 0 && emptyCount === 0) {
        return;
      }

      const name = rawName || 'Unspecified';
      const existing = summaryMap.get(name) || { variantName: name, filledCount: 0, emptyCount: 0 };
      existing.filledCount += filledCount;
      existing.emptyCount += emptyCount;
      summaryMap.set(name, existing);
    });

    return Array.from(summaryMap.values()).sort((a, b) => a.variantName.localeCompare(b.variantName));
  }

  private setVariantSummaries(transactions: DayBook[]): void {
    this.variantSummaries = this.buildVariantSummaries(transactions);
    this.filledVariantSummaries = this.variantSummaries
      .filter(summary => summary.filledCount > 0)
      .map(summary => ({ variantName: summary.variantName, filledCount: summary.filledCount }));
    this.emptyVariantSummaries = this.variantSummaries
      .filter(summary => summary.emptyCount > 0)
      .map(summary => ({ variantName: summary.variantName, emptyCount: summary.emptyCount }));
  }

  private setPaymentModeBreakdown(transactions: DayBook[]): void {
    this.paymentModeBreakdown = this.buildPaymentModeBreakdownByType(transactions);
  }

  private setTypePaymentModeBreakdowns(transactions: DayBook[]): void {
    this.salePaymentModeBreakdown = this.buildPaymentModeBreakdownByType(transactions, 'SALE');
    this.emptyReturnPaymentModeBreakdown = this.buildPaymentModeBreakdownByType(transactions, 'EMPTY_RETURN');
  }

  private buildPaymentModeBreakdownByType(transactions: DayBook[], type?: string): Array<{ mode: string; amount: number; count: number }> {
    const summaryMap = new Map<string, { mode: string; amount: number; count: number }>();
    (transactions || []).forEach(tx => {
      if (type && tx?.transactionType !== type) {
        return;
      }
      const splits = tx?.paymentSplits || [];
      if (splits.length > 0) {
        splits.forEach(split => {
          const splitAmount = Number(split?.amount) || 0;
          if (splitAmount <= 0) {
            return;
          }
          const rawSplitMode = typeof split?.paymentMode === 'string' ? split.paymentMode.trim() : '';
          const splitMode = rawSplitMode || 'Unknown';
          const splitKey = splitMode.toLowerCase();
          const splitExisting = summaryMap.get(splitKey) || { mode: splitMode, amount: 0, count: 0 };
          splitExisting.amount += splitAmount;
          splitExisting.count += 1;
          summaryMap.set(splitKey, splitExisting);
        });
        return;
      }

      const received = Number(tx?.amountReceived) || 0;
      if (received <= 0) {
        return;
      }
      const rawMode = typeof tx?.paymentMode === 'string' ? tx.paymentMode.trim() : '';
      const mode = rawMode || 'Unknown';
      const key = mode.toLowerCase();
      const existing = summaryMap.get(key) || { mode, amount: 0, count: 0 };
      existing.amount += received;
      existing.count += 1;
      summaryMap.set(key, existing);
    });
    return Array.from(summaryMap.values()).sort((a, b) => b.amount - a.amount);
  }

  openPaymentModeBreakdown(type: 'PAYMENT' | 'SALE' | 'EMPTY_RETURN' = 'PAYMENT') {
    const titleMap: Record<'PAYMENT' | 'SALE' | 'EMPTY_RETURN', string> = {
      PAYMENT: 'Payment Collection Breakdown',
      SALE: 'Sale Payment Breakdown',
      EMPTY_RETURN: 'Empty Return Payment Breakdown'
    };
    const breakdownMap: Record<'PAYMENT' | 'SALE' | 'EMPTY_RETURN', Array<{ mode: string; amount: number; count: number }>> = {
      PAYMENT: this.paymentModeBreakdown,
      SALE: this.salePaymentModeBreakdown,
      EMPTY_RETURN: this.emptyReturnPaymentModeBreakdown
    };
    const breakdown = breakdownMap[type] || [];
    if (breakdown.length === 0) {
      return;
    }
    this.paymentBreakdownTitle = titleMap[type];
    this.activePaymentBreakdown = breakdown;
    this.showPaymentModeBreakdownModal = true;
  }

  closePaymentModeBreakdown() {
    this.showPaymentModeBreakdownModal = false;
    this.activePaymentBreakdown = [];
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
