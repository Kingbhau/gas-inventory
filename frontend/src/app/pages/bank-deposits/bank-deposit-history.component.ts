import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faEdit, faTrash, faClipboard, faDownload } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { Subject, takeUntil } from 'rxjs';

import { BankDepositService } from '../../services/bank-deposit.service';
import { BankAccountService } from '../../services/bank-account.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { LoadingService } from '../../services/loading.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { SharedModule } from '../../shared/shared.module';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { BankDeposit } from '../../models/bank-deposit.model';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { BankDepositSummary } from '../../models/bank-deposit-summary.model';
import { PageResponse } from '../../models/page-response';
import { exportBankDepositsReportToPDF } from '../reports/export-bank-deposits-report.util';

@Component({
  selector: 'app-bank-deposit-history',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './bank-deposit-history.component.html',
  styleUrl: './bank-deposit-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BankDepositHistoryComponent implements OnInit, OnDestroy {
  @ViewChild('editModal') editModal!: TemplateRef<unknown>;
  
  faEye = faEye;
  faEdit = faEdit;
  faTrash = faTrash;
  faClipboard = faClipboard;
  faDownload = faDownload;

  // Modal properties
  showEditModal = false;
  editingDeposit: BankDeposit | null = null;
  editForm!: FormGroup;
  isEditSubmitting = false;
  
  // View details modal properties
  selectedDeposit: BankDeposit | null = null;

  // Filter properties
  filterFromDate = '';
  filterToDate = '';
  filterBankAccountId: number | null = null;
  filterPaymentMode = '';
  filterReferenceNumber = '';
  filterSearchTerm = '';
  filterCreatedBy = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  totalElements = 0;
  pageSizeOptions = [5, 10, 20, 50];

  // Data
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  deposits: BankDeposit[] = [];
  paginatedDeposits: BankDeposit[] = [];
  users: User[] = [];
  
  // Summary
  totalAmount = 0;
  
  private destroy$ = new Subject<void>();

  get isOwner(): boolean {
    const userInfo = this.authService.getUserInfo();
    return userInfo?.role === 'OWNER';
  }

  constructor(
    private bankDepositService: BankDepositService,
    private bankAccountService: BankAccountService,
    private paymentModeService: PaymentModeService,
    private dateUtility: DateUtilityService,
    private loadingService: LoadingService,
    private authService: AuthService,
    private userService: UserService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder
  ) {
    this.initEditForm();
  }

  ngOnInit() {
    this.loadBankAccounts();
    this.loadPaymentModes();
    this.loadUsers();
    this.loadDeposits();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all bank accounts
   */
  loadBankAccounts() {
    this.bankAccountService.getAllBankAccountsAll()
      .pipe(takeUntil(this.destroy$))
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

  /**
   * Load payment modes
   */
  loadPaymentModes() {
    this.paymentModeService.getAllPaymentModesAll()
      .pipe(takeUntil(this.destroy$))
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

  /**
   * Load deposits with pagination and filtering
   */
  loadDeposits() {
    this.loadingService.show('Loading deposits...');
    
    this.bankDepositService.getDeposits(
      this.currentPage - 1,
      this.pageSize,
      'depositDate',
      'DESC',
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      this.filterBankAccountId || undefined,
      this.filterPaymentMode || undefined,
      this.filterReferenceNumber || undefined,
      this.filterCreatedBy || undefined
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PageResponse<BankDeposit>) => {
          this.deposits = response.items || [];
          this.totalElements = response.totalElements ?? 0;
          this.totalPages = response.totalPages ?? 1;
          this.updatePaginatedDeposits();
          this.loadingService.hide();
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.toastr.error('Failed to load bank deposits');
          this.deposits = [];
          this.loadingService.hide();
        }
      });

    // Fetch summary for ALL filtered data (separate API call)
    this.loadDepositSummary();
  }

  /**
   * Load deposit summary from backend for all filtered data
   */
  private loadDepositSummary() {
    this.bankDepositService.getDepositSummary(
      this.filterFromDate,
      this.filterToDate,
      this.filterBankAccountId || undefined,
      this.filterPaymentMode || undefined,
      this.filterReferenceNumber || undefined,
      this.filterCreatedBy || undefined
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary: BankDepositSummary) => {
          this.totalAmount = summary.totalAmount || 0;
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.totalAmount = 0;
        }
      });
  }

  /**
   * Update paginated deposits list
   */
  updatePaginatedDeposits() {
    const start = 0;
    const end = this.pageSize;
    this.paginatedDeposits = this.deposits.slice(start, end);
  }

  /**
   * Apply filters
   */
  applyFilters() {
    this.currentPage = 1;
    this.loadDeposits();
  }

  /**
   * Reset all filters
   */
  resetFilters() {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterBankAccountId = null;
    this.filterPaymentMode = '';
    this.filterReferenceNumber = '';
    this.filterSearchTerm = '';
    this.filterCreatedBy = '';
    this.currentPage = 1;
    this.loadDeposits();
  }

  /**
   * Handle page size change
   */
  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 1;
    this.loadDeposits();
  }

  /**
   * Go to previous page
   */
  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadDeposits();
    }
  }

  /**
   * Go to next page
   */
  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadDeposits();
    }
  }

  /**
   * Get bank account name by ID
   */
  getBankAccountName(accountId: number | null): string {
    if (!accountId) {
      return 'N/A';
    }
    const account = this.bankAccounts.find(ba => ba.id === accountId);
    return account ? `${account.bankName} - ${account.accountNumber}` : 'N/A';
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
      .pipe(takeUntil(this.destroy$))
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

  /**
   * Handle bank account filter change
   */
  onBankAccountChange(value: string | number | null) {
    // Ensure the value is a number or null
    if (value === null || value === '' || value === undefined) {
      this.filterBankAccountId = null;
    } else {
      this.filterBankAccountId = typeof value === 'string' ? parseInt(value, 10) : value;
    }
  }

  /**
   * Get status badge class
   */
  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'CLEARED':
        return 'status-cleared';
      default:
        return 'status-pending';
    }
  }

  /**
   * Initialize edit form
   */
  private initEditForm() {
    this.editForm = this.fb.group({
      bankAccountId: [null, Validators.required],
      depositDate: ['', Validators.required],
      depositAmount: [null, [Validators.required, Validators.min(1)]],
      referenceNumber: [''],
      paymentMode: ['', Validators.required],
      notes: ['']
    });
  }

  /**
   * Edit deposit - Show modal
   */
  editDeposit(deposit: BankDeposit) {
    if (!deposit.id) {
      this.toastr.error('Cannot edit deposit without ID');
      return;
    }
    
    this.editingDeposit = deposit;
    this.editForm.patchValue({
      bankAccountId: deposit.bankAccountId,
      depositDate: this.dateUtility.getLocalDateString(new Date(deposit.depositDate)),
      depositAmount: deposit.depositAmount,
      referenceNumber: deposit.referenceNumber,
      paymentMode: deposit.paymentMode,
      notes: deposit.notes
    });
    
    this.showEditModal = true;
    this.cdr.markForCheck();
  }

  /**
   * Close edit modal
   */
  closeEditModal() {
    this.showEditModal = false;
    this.editingDeposit = null;
    this.editForm.reset();
    this.cdr.markForCheck();
  }

  /**
   * View deposit details
   */
  viewDeposit(deposit: BankDeposit) {
    this.selectedDeposit = deposit;
    this.cdr.markForCheck();
  }

  /**
   * Close view details modal
   */
  closeViewModal() {
    this.selectedDeposit = null;
    this.cdr.markForCheck();
  }

  /**
   * Save edited deposit
   */
  saveEditedDeposit() {
    if (this.editForm.invalid || !this.editingDeposit?.id) {
      this.toastr.error('Please fill all required fields correctly');
      return;
    }

    if (this.isEditSubmitting) {
      return;
    }

    this.isEditSubmitting = true;
    this.loadingService.show('Updating deposit...');

    const formData = this.editForm.value;
    const updatedDeposit: BankDeposit = {
      bankAccountId: formData.bankAccountId,
      depositDate: formData.depositDate,
      depositAmount: parseFloat(formData.depositAmount),
      referenceNumber: formData.referenceNumber,
      paymentMode: formData.paymentMode,
      notes: formData.notes || ''
    };

    this.bankDepositService.updateDeposit(String(this.editingDeposit.id), updatedDeposit)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isEditSubmitting = false;
          this.loadingService.hide();
          this.toastr.success('Bank deposit updated successfully!');
          this.closeEditModal();
          this.loadDeposits();
        },
        error: (error: unknown) => {
          this.isEditSubmitting = false;
          this.loadingService.hide();
          const err = error as { error?: { message?: string } };
          const errorMsg = err?.error?.message || 'Failed to update bank deposit';
          this.toastr.error(errorMsg);
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Delete deposit
   */
  deleteDeposit(deposit: BankDeposit) {
    if (!deposit.id) return;
    
    if (confirm('Are you sure you want to delete this deposit record?')) {
      this.loadingService.show('Deleting deposit...');
      
      this.bankDepositService.deleteDeposit(String(deposit.id))
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.toastr.success('Deposit deleted successfully');
            this.loadingService.hide();
            this.loadDeposits();
          },
          error: (error: unknown) => {
            this.toastr.error('Failed to delete deposit');
            this.loadingService.hide();
          }
        });
    }
  }

  /**
   * Export to CSV
   */
  exportToCsv() {
    if (this.deposits.length === 0) {
      this.toastr.info('No data to export');
      return;
    }

    const headers = ['Date', 'Bank Account', 'Amount', 'Mode', 'Reference #'];
    const rows = this.deposits.map(d => [
      new Date(d.depositDate).toLocaleDateString(),
      this.getBankAccountName(d.bankAccountId),
      d.depositAmount,
      d.paymentMode,
      d.referenceNumber
    ]);

    const csv = [headers, ...rows].map(row => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `bank-deposits-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.toastr.success('Export completed');
  }

  /**
   * Export to PDF
   */
  exportToPdf() {
    if (this.deposits.length === 0) {
      this.toastr.info('No data to export');
      return;
    }

    try {
      // Prepare data for PDF export
      const depositsForPdf = this.deposits.map(d => ({
        ...d,
        bankAccountName: this.getBankAccountName(d.bankAccountId)
      }));

      // Use summary totals from backend (for all filtered data, not just current page)
      const avgDepositValue = this.totalElements > 0 ? this.totalAmount / this.totalElements : 0;

      exportBankDepositsReportToPDF({
        deposits: depositsForPdf,
        fromDate: this.filterFromDate || undefined,
        toDate: this.filterToDate || undefined,
        bankAccountName: this.filterBankAccountId 
          ? this.getBankAccountName(this.filterBankAccountId) 
          : undefined,
        paymentMode: this.filterPaymentMode || undefined,
        referenceNumber: this.filterReferenceNumber || undefined,
        totalAmount: this.totalAmount,
        avgDepositValue: avgDepositValue,
        depositCount: this.totalElements,
        businessName: 'GAS AGENCY SYSTEM'
      });

      this.toastr.success('PDF exported successfully');
    } catch (error) {
      this.toastr.error('Failed to export PDF');
    }
  }
}
