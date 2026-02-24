import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPlus, faSearch, faEdit, faTrash, faEye, faTimes, faChevronDown, faSignOut, faUsers, faExclamation } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { AuthService } from '../../services/auth.service';
import { DataRefreshService } from '../../services/data-refresh.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { LoadingService } from '../../services/loading.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, finalize, of } from 'rxjs';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { SharedModule } from '../../shared/shared.module';
import { Customer } from '../../models/customer.model';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { PageResponse } from '../../models/page-response';
import { PaymentRequest } from '../../models/payment-request.model';

type PaymentCustomer = Customer & { showMenu?: boolean; dueAmount?: number };

@Component({
  selector: 'app-payment-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule],
  templateUrl: './payment-management.component.html',
  styleUrl: './payment-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentManagementComponent implements OnInit {
  faPlus = faPlus;
  faSearch = faSearch;
  faEdit = faEdit;
  faTrash = faTrash;
  faEye = faEye;
  faTimes = faTimes;
  faChevronDown = faChevronDown;
  faSignOut = faSignOut;
  faUsers = faUsers;
  faExclamation = faExclamation;

  userName: string = '';

  customers: PaymentCustomer[] = [];
  filteredCustomers: PaymentCustomer[] = [];
  searchTerm = '';
  selectedCustomer: PaymentCustomer | null = null;
  paymentPage = 1;
  paymentPageSize = 10;
  paymentTotalPages = 1;
  paymentTotalElements = 0;
  pageSizeOptions = [5, 10, 20, 50, 100];

  showPaymentForm = false;
  paymentForm: { amount: string | number | null; paymentDate: string; paymentMode: string; bankAccountId?: number | null } = {
    amount: null,
    paymentDate: '',
    paymentMode: ''
  };
  paymentError = '';
  isSubmittingPayment = false;
  paymentModes: PaymentMode[] = [];
  bankAccounts: BankAccount[] = [];
  ledgerEntries: CustomerCylinderLedger[] = [];
  isStaff = false;
  showDuplicatePaymentConfirm = false;
  duplicatePaymentInfo: {
    customerName: string;
    amount: number;
    paymentDate: string;
    paymentMode: string;
    bankAccountLabel?: string;
  } | null = null;
  pendingPaymentRequest: PaymentRequest | null = null;

  constructor(
    private customerService: CustomerService,
    private ledgerService: CustomerCylinderLedgerService,
    private bankAccountService: BankAccountService,
    private paymentModeService: PaymentModeService,
    private authService: AuthService,
    private dataRefreshService: DataRefreshService,
    private dateUtility: DateUtilityService,
    private loadingService: LoadingService,
    private router: Router,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {
    this.paymentForm.paymentDate = this.dateUtility.getTodayInIST();
  }

  ngOnInit() {
    this.userName = this.authService.getLoggedInUserName();
    const role = this.authService.getUserInfo()?.role || '';
    this.isStaff = role === 'STAFF';
    this.loadCustomers();
    this.loadBankAccounts();
    this.loadPaymentModes();
  }

  loadBankAccounts() {
    this.loadingService.show('Loading bank accounts...');
    this.bankAccountService.getActiveBankAccounts()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: BankAccount[]) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.bankAccounts = [];
        }
      });
  }

  loadPaymentModes() {
    this.loadingService.show('Loading payment modes...');
    this.paymentModeService.getActivePaymentModes()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: PaymentMode[]) => {
          this.paymentModes = response || [];
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.paymentModes = [];
        }
      });
  }

  getSelectedPaymentMode(): PaymentMode | undefined {
    return this.paymentModes.find(mode => mode.name === this.paymentForm.paymentMode);
  }

  isBankAccountRequired(): boolean {
    const selectedMode = this.getSelectedPaymentMode();
    return selectedMode ? selectedMode.isBankAccountRequired || false : false;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  loadCustomers() {
    const search = this.searchTerm?.trim() || undefined;
    this.loadingService.show('Loading customers...');
    this.customerService
      .getActiveCustomersPaged(this.paymentPage - 1, this.paymentPageSize, 'name', 'ASC', search, 0.01)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: PageResponse<Customer>) => {
          const content = data.items || [];
          this.customers = content;
          this.filteredCustomers = content;
          this.paymentTotalElements = data.totalElements ?? content.length;
          this.paymentTotalPages = data.totalPages ?? 1;
          this.cdr.markForCheck();
        },
        error: () => {
          this.toastr.error('Failed to load customers', 'Error');
        }
      });
  }

  filterCustomers() {
    this.paymentPage = 1;
    this.loadCustomers();
  }

  get paginatedCustomers() {
    return this.filteredCustomers;
  }

  onPageSizeChange(size: number) {
    this.paymentPageSize = Number(size);
    this.paymentPage = 1;
    this.loadCustomers();
  }

  previousPage() {
    if (this.paymentPage > 1) {
      this.paymentPage--;
      this.loadCustomers();
    }
  }

  nextPage() {
    if (this.paymentPage < this.paymentTotalPages) {
      this.paymentPage++;
      this.loadCustomers();
    }
  }

  selectCustomer(customer: PaymentCustomer) {
    if (!customer.id) {
      this.toastr.error('Invalid customer selected', 'Error');
      return;
    }
    this.selectedCustomer = customer;
    this.loadLedgerForCustomer(customer.id);
    this.cdr.markForCheck();
  }

  loadLedgerForCustomer(customerId: number) {
    this.loadingService.show('Loading ledger...');
    this.ledgerService.getLedgerByCustomer(customerId)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe(
        (data: CustomerCylinderLedger[]) => {
          this.ledgerEntries = data;
          this.cdr.markForCheck();
        },
        (error: unknown) => {
          this.toastr.error('Failed to load ledger', 'Error');
        }
      );
  }

  openPaymentForm() {
    if (!this.selectedCustomer) {
      this.toastr.warning('Please select a customer first', 'Warning');
      return;
    }
    this.paymentForm = {
      amount: null,
      paymentDate: this.dateUtility.getTodayInIST(),
      paymentMode: '',
      bankAccountId: null
    };
    if (this.isStaff) {
      this.paymentForm.paymentDate = this.dateUtility.getTodayInIST();
    }
    this.paymentError = '';
    this.showPaymentForm = true;
    this.cdr.markForCheck();
  }

  closePaymentForm() {
    this.showPaymentForm = false;
    this.paymentForm = { amount: null, paymentDate: '', paymentMode: '', bankAccountId: null };
    this.paymentError = '';
    this.cdr.markForCheck();
  }

  submitPayment() {
    if (this.isStaff) {
      this.paymentForm.paymentDate = this.dateUtility.getTodayInIST();
    }
    if (!this.paymentForm.amount) {
      this.toastr.error('Please enter a valid payment amount', 'Validation Error');
      return;
    }

    if (!this.paymentForm.paymentDate) {
      this.toastr.error('Please select a payment date', 'Validation Error');
      return;
    }

    if (!this.paymentForm.paymentMode) {
      this.toastr.error('Please select a payment mode', 'Validation Error');
      return;
    }

    // If payment mode requires bank account, validate it's selected
    const selectedMode = this.getSelectedPaymentMode();
    if (selectedMode?.isBankAccountRequired && !this.paymentForm.bankAccountId) {
      this.toastr.error('Please select a bank account for this payment mode', 'Validation Error');
      return;
    }

    // Convert amount string (formatted like "1,00,000.00") to number
    const amountValue = typeof this.paymentForm.amount === 'string' 
      ? parseFloat(this.paymentForm.amount.replace(/,/g, ''))
      : this.paymentForm.amount;

    // Validate amount is a valid number
    if (isNaN(amountValue) || amountValue <= 0) {
      this.toastr.error('Please enter a valid payment amount', 'Validation Error');
      return;
    }

    // Get the current due amount from the selected customer
    const currentDue = this.selectedCustomer?.dueAmount || 0;

    // Validate payment amount doesn't exceed due amount
    if (amountValue > currentDue) {
      this.toastr.error(`Payment amount cannot exceed due amount of ₹${currentDue.toFixed(2)}. Current payment: ₹${amountValue.toFixed(2)}`, 'Validation Error');
      return;
    }

    this.isSubmittingPayment = true;
    this.loadingService.show('Recording payment...');
    if (!this.selectedCustomer?.id) {
      this.toastr.error('No customer selected', 'Error');
      this.isSubmittingPayment = false;
      return;
    }
    const paymentData: PaymentRequest = {
      customerId: this.selectedCustomer.id,
      amount: amountValue,
      paymentDate: this.paymentForm.paymentDate,
      paymentMode: this.paymentForm.paymentMode
    };

    // Add bankAccountId if payment mode requires bank account
    if (selectedMode?.isBankAccountRequired && this.paymentForm.bankAccountId) {
      paymentData.bankAccountId = this.paymentForm.bankAccountId;
    }

    this.checkDuplicateAndSubmitPayment(paymentData);
  }

  private parseAmountInput(value: unknown): number {
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      const cleaned = value.replace(/,/g, '').trim();
      const parsed = parseFloat(cleaned);
      return isNaN(parsed) ? 0 : parsed;
    }
    return 0;
  }

  private normalizeAmount(value: number | string | null | undefined): number {
    const parsed = this.parseAmountInput(value);
    return Math.round(parsed * 100) / 100;
  }

  private normalizeId(value: number | string | null | undefined): number | null {
    if (value === null || typeof value === 'undefined') return null;
    const parsed = Number(value);
    return isNaN(parsed) ? null : parsed;
  }

  private getLatestPaymentEntry(entries: CustomerCylinderLedger[] = this.ledgerEntries): CustomerCylinderLedger | null {
    const payments = (entries || []).filter(entry => entry.refType === 'PAYMENT');
    if (payments.length === 0) return null;
    const getTimestamp = (entry: CustomerCylinderLedger) => {
      if (entry.createdAt) {
        const ts = Date.parse(entry.createdAt);
        if (!isNaN(ts)) return ts;
      }
      if (entry.transactionDate) {
        const ts = Date.parse(entry.transactionDate);
        if (!isNaN(ts)) return ts;
      }
      return 0;
    };
    return payments.sort((a, b) => {
      const timeDiff = getTimestamp(b) - getTimestamp(a);
      if (timeDiff !== 0) return timeDiff;
      return (b.id || 0) - (a.id || 0);
    })[0];
  }

  private isDuplicatePayment(lastEntry: CustomerCylinderLedger | null, request: PaymentRequest): boolean {
    if (!lastEntry) return false;
    const sameCustomer = lastEntry.customerId === request.customerId;
    const sameAmount = this.normalizeAmount(lastEntry.amountReceived) === this.normalizeAmount(request.amount);
    const sameDate = String(lastEntry.transactionDate || '') === String(request.paymentDate || '');
    const sameMode = String(lastEntry.paymentMode || '') === String(request.paymentMode || '');
    const sameBankAccount = this.normalizeId(lastEntry.bankAccountId) === this.normalizeId(request.bankAccountId);
    return sameCustomer && sameAmount && sameDate && sameMode && sameBankAccount;
  }

  private buildDuplicatePaymentInfo(request: PaymentRequest) {
    const bankAccountId = this.normalizeId(request.bankAccountId);
    const bankAccount = this.bankAccounts.find(acc => acc.id === bankAccountId);
    return {
      customerName: this.selectedCustomer?.name || 'Customer',
      amount: this.normalizeAmount(request.amount),
      paymentDate: String(request.paymentDate || ''),
      paymentMode: request.paymentMode || '',
      bankAccountLabel: bankAccount
        ? `${bankAccount.bankName} - ${bankAccount.accountNumber}`
        : undefined
    };
  }

  private checkDuplicateAndSubmitPayment(request: PaymentRequest): void {
    const lastEntry = this.getLatestPaymentEntry();
    if (lastEntry) {
      if (this.isDuplicatePayment(lastEntry, request)) {
        this.pendingPaymentRequest = request;
        this.duplicatePaymentInfo = this.buildDuplicatePaymentInfo(request);
        this.showDuplicatePaymentConfirm = true;
        this.isSubmittingPayment = false;
        this.loadingService.hide();
        this.cdr.markForCheck();
        return;
      }
      this.createPayment(request);
      return;
    }

    this.ledgerService.getLedgerByCustomerAll(request.customerId)
      .pipe(
        catchError(() => of([] as CustomerCylinderLedger[]))
      )
      .subscribe((entries: CustomerCylinderLedger[]) => {
        const fetchedLast = this.getLatestPaymentEntry(entries);
        if (fetchedLast && this.isDuplicatePayment(fetchedLast, request)) {
          this.pendingPaymentRequest = request;
          this.duplicatePaymentInfo = this.buildDuplicatePaymentInfo(request);
          this.showDuplicatePaymentConfirm = true;
          this.isSubmittingPayment = false;
          this.loadingService.hide();
          this.cdr.markForCheck();
          return;
        }
        this.createPayment(request);
      });
  }

  confirmDuplicatePayment(): void {
    if (!this.pendingPaymentRequest) {
      this.showDuplicatePaymentConfirm = false;
      this.duplicatePaymentInfo = null;
      return;
    }
    const request = this.pendingPaymentRequest;
    this.pendingPaymentRequest = null;
    this.showDuplicatePaymentConfirm = false;
    this.duplicatePaymentInfo = null;
    this.createPayment(request);
  }

  cancelDuplicatePayment(): void {
    this.pendingPaymentRequest = null;
    this.showDuplicatePaymentConfirm = false;
    this.duplicatePaymentInfo = null;
    this.isSubmittingPayment = false;
    this.loadingService.hide();
    this.cdr.markForCheck();
  }

  private createPayment(paymentData: PaymentRequest): void {
    this.ledgerService.recordPayment(paymentData)
      .pipe(
        finalize(() => this.loadingService.hide()),
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error recording payment';
          this.toastr.error(errorMessage, 'Error');
          this.isSubmittingPayment = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: CustomerCylinderLedger | null) => {
        if (response) {
          this.toastr.success('Payment recorded successfully', 'Success');
          // Notify dashboard of the payment
          this.dataRefreshService.notifyPaymentReceived(response);
          this.closePaymentForm();
          // Refresh ledger
          if (this.selectedCustomer?.id) {
            this.loadLedgerForCustomer(this.selectedCustomer.id);
          }
          // Refresh customers to update due amounts
          this.loadCustomers();
        }
        this.isSubmittingPayment = false;
        this.cdr.markForCheck();
      });
  }

  getCurrentDue(): number {
    return this.selectedCustomer?.dueAmount || 0;
  }

  formatMovementType(type: string): string {
    const typeMap: { [key: string]: string } = {
      'SALE': 'Sale',
      'INITIAL_STOCK': 'Initial Stock',
      'EMPTY_RETURN': 'Empty Return',
      'TRANSFER': 'Transfer',
      'PAYMENT': 'Payment'
    };
    return typeMap[type] || type;
  }

  getMovementTypeClass(type: string): string {
    const classMap: { [key: string]: string } = {
      'SALE': 'sale',
      'INITIAL_STOCK': 'initial_stock',
      'EMPTY_RETURN': 'empty_return',
      'TRANSFER': 'transfer',
      'PAYMENT': 'payment'
    };
    return classMap[type] || 'default';
  }

  getPaymentModeLabel(mode: string): string {
    const modeMap: { [key: string]: string } = {
      'CASH': 'Cash',
      'CHEQUE': 'Cheque',
      'BANK_TRANSFER': 'Bank Transfer',
      'UPI': 'UPI'
    };
    return modeMap[mode] || mode;
  }

  clearSearch() {
    this.searchTerm = '';
    this.filterCustomers();
  }

  deselectCustomer() {
    this.selectedCustomer = null;
    this.ledgerEntries = [];
    this.cdr.markForCheck();
  }

  selectAndPayment(customer: PaymentCustomer) {
    this.selectCustomer(customer);
    setTimeout(() => {
      this.openPaymentForm();
    }, 100);
  }
}
