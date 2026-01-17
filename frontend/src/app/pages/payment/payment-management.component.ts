import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPlus, faSearch, faPencil, faTrash, faEye, faTimes, faChevronDown, faSignOut, faUsers, faExclamation } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, of } from 'rxjs';
import { BankAccount } from '../../models/bank-account.model';

@Component({
  selector: 'app-payment-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule],
  templateUrl: './payment-management.component.html',
  styleUrl: './payment-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentManagementComponent implements OnInit {
  faPlus = faPlus;
  faSearch = faSearch;
  faPencil = faPencil;
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

  showPaymentForm = false;
  paymentForm: { amount: number | null; paymentDate: string; paymentMode: string; bankAccountId?: number | null } = {
    amount: null,
    paymentDate: new Date().toISOString().split('T')[0],
    paymentMode: ''
  };
  paymentError = '';
  isSubmittingPayment = false;
  paymentModes = ['Cash', 'Cheque', 'Bank Transfer', 'UPI'];
  bankAccounts: BankAccount[] = [];
  ledgerEntries: any[] = [];

  constructor(
    private customerService: CustomerService,
    private ledgerService: CustomerCylinderLedgerService,
    private bankAccountService: BankAccountService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.userName = this.authService.getLoggedInUserName();
    this.loadCustomers();
    this.loadBankAccounts();
  }

  loadBankAccounts() {
    this.bankAccountService.getActiveBankAccounts()
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

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  loadCustomers() {
    this.customerService.getActiveCustomers().subscribe(
      (data: any[]) => {
        this.customers = data;
        // Enrich customers with due amounts
        data.forEach((customer: any) => {
          this.ledgerService.getLedgerByCustomer(customer.id).subscribe(
            (ledger: any[]) => {
              if (ledger && ledger.length > 0) {
                customer.dueAmount = ledger[ledger.length - 1].dueAmount || 0;
              } else {
                customer.dueAmount = 0;
              }
              this.cdr.markForCheck();
            },
            (error: any) => {
              customer.dueAmount = 0;
              this.cdr.markForCheck();
            }
          );
        });
        this.filteredCustomers = data;
        this.cdr.markForCheck();
      },
      (error: any) => {
        this.toastr.error('Failed to load customers', 'Error');
      }
    );
  }

  filterCustomers() {
    if (!this.searchTerm.trim()) {
      this.filteredCustomers = this.customers;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredCustomers = this.customers.filter(c =>
        c.name.toLowerCase().includes(term) ||
        c.mobile?.includes(this.searchTerm)
      );
    }
    this.cdr.markForCheck();
  }

  selectCustomer(customer: any) {
    this.selectedCustomer = customer;
    this.loadLedgerForCustomer(customer.id);
    this.cdr.markForCheck();
  }

  loadLedgerForCustomer(customerId: number) {
    this.ledgerService.getLedgerByCustomer(customerId).subscribe(
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
      paymentDate: new Date().toISOString().split('T')[0],
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
    this.paymentError = '';

    if (!this.paymentForm.amount || this.paymentForm.amount <= 0) {
      this.paymentError = 'Please enter a valid payment amount';
      this.cdr.markForCheck();
      return;
    }

    if (!this.paymentForm.paymentDate) {
      this.paymentError = 'Please select a payment date';
      this.cdr.markForCheck();
      return;
    }

    if (!this.paymentForm.paymentMode) {
      this.paymentError = 'Please select a payment mode';
      this.cdr.markForCheck();
      return;
    }

    // If payment mode is not CASH, bank account is required
    if (this.paymentForm.paymentMode.toUpperCase() !== 'CASH' && !this.paymentForm.bankAccountId) {
      this.paymentError = 'Please select a bank account for non-cash payments';
      this.cdr.markForCheck();
      return;
    }

    // Get the current due amount from the most recent ledger entry
    const currentDue = this.ledgerEntries.length > 0
      ? this.ledgerEntries[this.ledgerEntries.length - 1].dueAmount || 0
      : 0;

    // Validate payment amount doesn't exceed due amount
    if (this.paymentForm.amount > currentDue) {
      this.paymentError = `Payment amount cannot exceed due amount of ₹${currentDue.toFixed(2)}. Current payment: ₹${this.paymentForm.amount.toFixed(2)}`;
      this.cdr.markForCheck();
      return;
    }

    this.isSubmittingPayment = true;
    const paymentData: any = {
      customerId: this.selectedCustomer.id,
      amount: this.paymentForm.amount,
      paymentDate: this.paymentForm.paymentDate,
      paymentMode: this.paymentForm.paymentMode
    };

    // Add bankAccountId if payment is not CASH
    if (this.paymentForm.paymentMode.toUpperCase() !== 'CASH' && this.paymentForm.bankAccountId) {
      paymentData.bankAccountId = this.paymentForm.bankAccountId;
    }

    this.ledgerService.recordPayment(paymentData)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording payment';
          this.paymentError = errorMessage;
          this.toastr.error(errorMessage, 'Error');
          this.isSubmittingPayment = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
          this.toastr.success('Payment recorded successfully', 'Success');
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
    if (this.ledgerEntries.length > 0) {
      return this.ledgerEntries[this.ledgerEntries.length - 1].dueAmount || 0;
    }
    return 0;
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
