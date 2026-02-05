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

  customers: any[] = [];
  filteredCustomers: any[] = [];
  searchTerm = '';
  selectedCustomer: any = null;
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
  ledgerEntries: any[] = [];

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
    this.loadCustomers();
    this.loadBankAccounts();
    this.loadPaymentModes();
  }

  loadBankAccounts() {
    this.loadingService.show('Loading bank accounts...');
    this.bankAccountService.getActiveBankAccounts()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: any) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading bank accounts:', error);
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
        error: (error: any) => {
          console.error('Error loading payment modes:', error);
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
        next: (data: any) => {
          const content = data.content || data || [];
          this.customers = content;
          this.filteredCustomers = content;
          this.paymentTotalElements = data.totalElements || content.length;
          this.paymentTotalPages = data.totalPages || 1;
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

  selectCustomer(customer: any) {
    this.selectedCustomer = customer;
    this.loadLedgerForCustomer(customer.id);
    this.cdr.markForCheck();
  }

  loadLedgerForCustomer(customerId: number) {
    this.loadingService.show('Loading ledger...');
    this.ledgerService.getLedgerByCustomer(customerId)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe(
        (data: any[]) => {
          this.ledgerEntries = data;
          this.cdr.markForCheck();
        },
        (error: any) => {
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
    const paymentData: any = {
      customerId: this.selectedCustomer.id,
      amount: amountValue,
      paymentDate: this.paymentForm.paymentDate,
      paymentMode: this.paymentForm.paymentMode
    };

    // Add bankAccountId if payment mode requires bank account
    if (selectedMode?.isBankAccountRequired && this.paymentForm.bankAccountId) {
      paymentData.bankAccountId = this.paymentForm.bankAccountId;
    }

    this.ledgerService.recordPayment(paymentData)
      .pipe(
        finalize(() => this.loadingService.hide()),
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording payment';
          this.toastr.error(errorMessage, 'Error');
          this.isSubmittingPayment = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
          this.toastr.success('Payment recorded successfully', 'Success');
          // Notify dashboard of the payment
          this.dataRefreshService.notifyPaymentReceived(response);
          this.closePaymentForm();
          // Refresh ledger
          this.loadLedgerForCustomer(this.selectedCustomer.id);
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

  selectAndPayment(customer: any) {
    this.selectCustomer(customer);
    setTimeout(() => {
      this.openPaymentForm();
    }, 100);
  }
}
