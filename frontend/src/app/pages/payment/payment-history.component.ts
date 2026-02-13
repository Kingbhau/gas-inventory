import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faClipboard } from '@fortawesome/free-solid-svg-icons';
import { finalize } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { SharedModule } from '../../shared/shared.module';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { CustomerService } from '../../services/customer.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { BankAccountService } from '../../services/bank-account.service';
import { LoadingService } from '../../services/loading.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { BankAccount } from '../../models/bank-account.model';
import { Customer } from '../../models/customer.model';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { PageResponse } from '../../models/page-response';
import { PaymentsSummary } from '../../models/payments-summary.model';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './payment-history.component.html',
  styleUrl: './payment-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentHistoryComponent implements OnInit, OnDestroy {
  faEye = faEye;
  faClipboard = faClipboard;

  // Filters
  filterFromDate = '';
  filterToDate = '';
  filterCustomerId: number | null = null;
  filterPaymentMode = '';
  filterBankAccountId: number | null = null;
  filterCreatedBy = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  totalElements = 0;
  pageSizeOptions = [5, 10, 20, 50, 100];

  // Data
  customersList: Customer[] = [];
  paymentModes: PaymentMode[] = [];
  bankAccounts: BankAccount[] = [];
  payments: CustomerCylinderLedger[] = [];
  paginatedPayments: CustomerCylinderLedger[] = [];
  users: User[] = [];

  // Summary
  totalAmount = 0;

  // View modal
  selectedPayment: CustomerCylinderLedger | null = null;

  constructor(
    private ledgerService: CustomerCylinderLedgerService,
    private customerService: CustomerService,
    private paymentModeService: PaymentModeService,
    private bankAccountService: BankAccountService,
    private loadingService: LoadingService,
    private userService: UserService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadCustomers();
    this.loadPaymentModes();
    this.loadBankAccounts();
    this.loadUsers();
    this.loadPayments();
  }

  ngOnDestroy() {}

  loadPayments() {
    this.loadingService.show('Loading payments...');
    this.ledgerService.getPayments(
      this.currentPage - 1,
      this.pageSize,
      'transactionDate',
      'DESC',
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      this.filterCustomerId || undefined,
      this.filterPaymentMode || undefined,
      this.filterBankAccountId || undefined,
      this.filterCreatedBy || undefined
    )
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: PageResponse<CustomerCylinderLedger>) => {
          this.payments = response?.items || [];
          this.totalElements = response?.totalElements ?? 0;
          this.totalPages = response?.totalPages ?? 1;
          this.updatePaginatedPayments();
          this.loadPaymentsSummary();
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Failed to load payments';
          this.toastr.error(errorMessage, 'Error');
          this.payments = [];
          this.totalElements = 0;
          this.totalPages = 1;
          this.updatePaginatedPayments();
          this.cdr.markForCheck();
        }
      });
  }

  private loadPaymentsSummary() {
    this.ledgerService.getPaymentsSummary(
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      this.filterCustomerId || undefined,
      this.filterPaymentMode || undefined,
      this.filterBankAccountId || undefined,
      this.filterCreatedBy || undefined
    )
      .subscribe({
        next: (summary: PaymentsSummary) => {
          this.totalAmount = summary?.totalAmount || 0;
          this.cdr.markForCheck();
        },
        error: () => {
          this.totalAmount = 0;
        }
      });
  }

  updatePaginatedPayments() {
    this.paginatedPayments = this.payments.slice(0, this.pageSize);
  }

  applyFilters() {
    this.currentPage = 1;
    this.loadPayments();
  }

  resetFilters() {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterCustomerId = null;
    this.filterPaymentMode = '';
    this.filterBankAccountId = null;
    this.filterCreatedBy = '';
    this.currentPage = 1;
    this.loadPayments();
  }

  onPageSizeChange(size: number) {
    this.pageSize = Number(size);
    this.currentPage = 1;
    this.loadPayments();
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadPayments();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadPayments();
    }
  }

  viewPayment(payment: CustomerCylinderLedger) {
    this.selectedPayment = payment;
  }

  closeViewModal() {
    this.selectedPayment = null;
  }

  loadCustomers() {
    this.customerService.getAllCustomersAll()
      .subscribe({
        next: (data: Customer[]) => {
          this.customersList = data || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.customersList = [];
        }
      });
  }

  loadPaymentModes() {
    this.paymentModeService.getAllPaymentModesAll()
      .subscribe({
        next: (response: PaymentMode[]) => {
          this.paymentModes = response || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.paymentModes = [];
        }
      });
  }

  loadBankAccounts() {
    this.bankAccountService.getAllBankAccountsAll()
      .subscribe({
        next: (response: BankAccount[]) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.bankAccounts = [];
        }
      });
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

  getBankAccountDisplayName(accountId: number | null, fallbackName?: string | null): string {
    if (accountId) {
      const account = this.bankAccounts.find(ba => ba.id === accountId);
      if (account) {
        return this.formatBankAccount(account);
      }
    }
    return fallbackName || 'N/A';
  }

  private formatBankAccount(account: BankAccount): string {
    const accountLabel = account.accountNumber;
    return accountLabel ? `${account.bankName} - ${accountLabel}` : account.bankName;
  }

  getCreatedByName(createdBy?: string | null): string {
    if (!createdBy) {
      return 'N/A';
    }
    const user = this.users.find(u => u.username === createdBy);
    return user?.name || createdBy;
  }

  onBankAccountChange(value: string | number | null) {
    if (value === null || value === '' || value === undefined) {
      this.filterBankAccountId = null;
    } else {
      this.filterBankAccountId = typeof value === 'string' ? parseInt(value, 10) : value;
    }
  }
}
