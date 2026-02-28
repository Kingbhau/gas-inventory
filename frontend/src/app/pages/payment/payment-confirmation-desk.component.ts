import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faClipboard } from '@fortawesome/free-solid-svg-icons';
import { finalize } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { SharedModule } from '../../shared/shared.module';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { LedgerVerificationSummary } from '../../models/ledger-verification-summary.model';
import { BankAccount } from '../../models/bank-account.model';
import { PageResponse } from '../../models/page-response';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { LoadingService } from '../../services/loading.service';
import { AuthService } from '../../services/auth.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { UserService } from '../../services/user.service';
import { PaymentMode } from '../../models/payment-mode.model';
import { User } from '../../models/user.model';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-payment-confirmation-desk',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './payment-confirmation-desk.component.html',
  styleUrl: './payment-confirmation-desk.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentConfirmationDeskComponent implements OnInit {
  @ViewChildren('deskFilterAutocomplete') deskFilterAutocompletes!: QueryList<AutocompleteInputComponent>;
  faClipboard = faClipboard;

  filterFromDate = '';
  filterToDate = '';
  filterTransactionType = '';
  filterPaymentMode = '';
  filterCreatedBy = '';
  filterBankAccountId: number | null = null;
  filterStatus = 'PENDING';
  filterSearch = '';

  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  totalElements = 0;
  pageSizeOptions = [10, 20, 50, 100];
  showActionModal = false;
  actionType: 'VERIFY' | 'PENDING' | 'BULK_VERIFY' | 'BULK_REJECT' = 'VERIFY';
  actionTarget: CustomerCylinderLedger | null = null;
  actionRemark = '';

  records: CustomerCylinderLedger[] = [];
  selectedRecordIds = new Set<number>();
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  users: User[] = [];
  transactionTypeOptions = [
    { value: '', label: 'All Types' },
    { value: 'SALE', label: 'Sale' },
    { value: 'EMPTY_RETURN', label: 'Empty Return' },
    { value: 'PAYMENT', label: 'Payment' }
  ];
  summary: LedgerVerificationSummary = {
    totalAmount: 0,
    pendingAmount: 0,
    verifiedAmount: 0,
    rejectedAmount: 0,
    pendingCount: 0,
    verifiedCount: 0,
    rejectedCount: 0
  };

  constructor(
    private ledgerService: CustomerCylinderLedgerService,
    private bankAccountService: BankAccountService,
    private loadingService: LoadingService,
    private authService: AuthService,
    private paymentModeService: PaymentModeService,
    private userService: UserService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadBankAccounts();
    this.loadPaymentModes();
    this.loadUsers();
    this.loadQueue();
  }

  get canVerify(): boolean {
    return (this.authService.getUserInfo()?.role || '') === 'OWNER';
  }

  loadQueue(): void {
    this.loadingService.show('Loading bank confirmation desk...');
    this.ledgerService.getBankConfirmationQueue(
      this.currentPage - 1,
      this.pageSize,
      'transactionDate',
      'DESC',
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      this.filterTransactionType || undefined,
      this.filterPaymentMode || undefined,
      this.filterCreatedBy || undefined,
      this.filterBankAccountId || undefined,
      this.filterStatus || undefined,
      this.filterSearch || undefined
    )
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: PageResponse<CustomerCylinderLedger>) => {
          this.records = response?.items || [];
          this.selectedRecordIds.clear();
          this.totalElements = response?.totalElements ?? 0;
          this.totalPages = response?.totalPages ?? 1;
          this.loadSummary();
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const message = err?.error?.message || err?.message || 'Failed to load payment verification list';
          this.toastr.error(message, 'Error');
          this.records = [];
          this.totalElements = 0;
          this.totalPages = 1;
          this.summary = {
            totalAmount: 0,
            pendingAmount: 0,
            verifiedAmount: 0,
            rejectedAmount: 0,
            pendingCount: 0,
            verifiedCount: 0,
            rejectedCount: 0
          };
          this.cdr.markForCheck();
        }
      });
  }

  loadSummary(): void {
    this.ledgerService.getBankConfirmationSummary(
      this.filterFromDate || undefined,
      this.filterToDate || undefined,
      this.filterTransactionType || undefined,
      this.filterPaymentMode || undefined,
      this.filterCreatedBy || undefined,
      this.filterBankAccountId || undefined,
      this.filterSearch || undefined
    ).subscribe({
      next: (summary) => {
        this.summary = summary || this.summary;
        this.cdr.markForCheck();
      },
      error: () => {
        this.summary = {
          totalAmount: 0,
          pendingAmount: 0,
          verifiedAmount: 0,
          rejectedAmount: 0,
          pendingCount: 0,
          verifiedCount: 0,
          rejectedCount: 0
        };
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadQueue();
  }

  resetFilters(): void {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterTransactionType = '';
    this.filterPaymentMode = '';
    this.filterCreatedBy = '';
    this.filterBankAccountId = null;
    this.filterStatus = 'PENDING';
    this.filterSearch = '';
    if (this.deskFilterAutocompletes && this.deskFilterAutocompletes.length > 0) {
      this.deskFilterAutocompletes.forEach(input => input.resetInput());
    }
    this.currentPage = 1;
    this.loadQueue();
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadQueue();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadQueue();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize = Number(size);
    this.currentPage = 1;
    this.loadQueue();
  }

  getStatusClass(status?: string): string {
    if (status === 'VERIFIED') return 'status-verified';
    if (status === 'REJECTED') return 'status-rejected';
    return 'status-pending';
  }

  getTransactionTypeLabel(refType?: string): string {
    const type = (refType || '').toUpperCase();
    if (type === 'SALE') return 'Sale';
    if (type === 'EMPTY_RETURN') return 'Empty Return';
    if (type === 'PAYMENT') return 'Payment';
    if (!type) return '-';
    return type.replace(/_/g, ' ');
  }

  getTransactionTypeClass(refType?: string): string {
    const type = (refType || '').toUpperCase();
    if (type === 'SALE') return 'type-sale';
    if (type === 'EMPTY_RETURN') return 'type-empty-return';
    if (type === 'PAYMENT') return 'type-payment';
    return 'type-default';
  }

  isPending(record: CustomerCylinderLedger): boolean {
    return (record.verificationStatus || 'PENDING') === 'PENDING';
  }

  toggleSelection(record: CustomerCylinderLedger, checked: boolean): void {
    if (!record.id || !this.isPending(record)) return;
    if (checked) {
      this.selectedRecordIds.add(record.id);
    } else {
      this.selectedRecordIds.delete(record.id);
    }
  }

  isSelected(record: CustomerCylinderLedger): boolean {
    return !!record.id && this.selectedRecordIds.has(record.id);
  }

  toggleSelectAllPending(checked: boolean): void {
    if (!checked) {
      this.selectedRecordIds.clear();
      return;
    }
    this.records
      .filter(record => this.isPending(record) && !!record.id)
      .forEach(record => this.selectedRecordIds.add(record.id as number));
  }

  get allPendingSelected(): boolean {
    const pendingIds = this.records
      .filter(record => this.isPending(record) && !!record.id)
      .map(record => record.id as number);
    return pendingIds.length > 0 && pendingIds.every(id => this.selectedRecordIds.has(id));
  }

  get selectedCount(): number {
    return this.selectedRecordIds.size;
  }

  bulkVerifySelected(): void {
    const ids = Array.from(this.selectedRecordIds.values());
    if (ids.length === 0) {
      this.toastr.info('Select at least one pending transaction', 'Info');
      return;
    }
    this.openActionModal('BULK_VERIFY', null);
  }

  bulkRejectSelected(): void {
    const ids = Array.from(this.selectedRecordIds.values());
    if (ids.length === 0) {
      this.toastr.info('Select at least one pending transaction', 'Info');
      return;
    }
    this.openActionModal('BULK_REJECT', null);
  }

  openActionModal(type: 'VERIFY' | 'PENDING' | 'BULK_VERIFY' | 'BULK_REJECT', record: CustomerCylinderLedger | null): void {
    this.actionType = type;
    this.actionTarget = record;
    this.actionRemark = '';
    this.showActionModal = true;
    this.cdr.markForCheck();
  }

  closeActionModal(): void {
    this.showActionModal = false;
    this.actionTarget = null;
    this.actionRemark = '';
    this.actionType = 'VERIFY';
    this.cdr.markForCheck();
  }

  confirmActionModal(): void {
    if (this.actionType === 'BULK_VERIFY' || this.actionType === 'BULK_REJECT') {
      const ids = Array.from(this.selectedRecordIds.values());
      if (ids.length === 0) {
        this.toastr.info('Select at least one pending transaction', 'Info');
        return;
      }
      if (this.actionType === 'BULK_REJECT' && !this.actionRemark.trim()) {
        this.toastr.info('Remark is required for bulk reject', 'Info');
        return;
      }

      const apiCall = this.actionType === 'BULK_REJECT'
        ? this.ledgerService.bulkRejectBankConfirmations(ids, this.actionRemark.trim())
        : this.ledgerService.bulkVerifyBankConfirmations(ids, this.actionRemark.trim() || undefined);

      apiCall
        .subscribe({
          next: () => {
            const successMsg = this.actionType === 'BULK_REJECT'
              ? `${ids.length} transaction(s) rejected`
              : `${ids.length} transaction(s) verified`;
            this.toastr.success(successMsg, 'Success');
            this.closeActionModal();
            this.loadQueue();
          },
          error: (error: unknown) => {
            const err = error as { error?: { message?: string }; message?: string };
            const errorMsg = this.actionType === 'BULK_REJECT' ? 'Bulk rejection failed' : 'Bulk verification failed';
            this.toastr.error(err?.error?.message || err?.message || errorMsg, 'Error');
          }
        });
      return;
    }

    if (!this.actionTarget?.id) {
      this.closeActionModal();
      return;
    }

    if (this.actionType === 'PENDING' && !this.actionRemark.trim()) {
      this.toastr.info('Remark is required to move status back to pending', 'Info');
      return;
    }

    if (this.actionType === 'VERIFY') {
      this.ledgerService.verifyBankConfirmation(this.actionTarget.id, this.actionRemark.trim() || undefined)
        .subscribe({
          next: () => {
            this.toastr.success('Transaction marked as verified', 'Success');
            this.closeActionModal();
            this.loadQueue();
          },
          error: (error: unknown) => {
            const err = error as { error?: { message?: string }; message?: string };
            this.toastr.error(err?.error?.message || err?.message || 'Verification failed', 'Error');
          }
        });
      return;
    }

    this.ledgerService.markBankConfirmationPending(this.actionTarget.id, this.actionRemark.trim())
      .subscribe({
        next: () => {
          this.toastr.success('Transaction moved back to pending', 'Success');
          this.closeActionModal();
          this.loadQueue();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          this.toastr.error(err?.error?.message || err?.message || 'Update failed', 'Error');
        }
      });
  }

  getActionTitle(): string {
    if (this.actionType === 'VERIFY') return 'Update Pending Transaction';
    if (this.actionType === 'PENDING') return 'Move Back To Pending';
    if (this.actionType === 'BULK_REJECT') return `Reject ${this.selectedCount} Selected Transactions`;
    return `Verify ${this.selectedCount} Selected Transactions`;
  }

  isRemarkRequired(): boolean {
    return this.actionType === 'PENDING' || this.actionType === 'BULK_REJECT';
  }

  isStatusToggleAllowed(record: CustomerCylinderLedger): boolean {
    if (!this.canVerify) return false;
    const status = record.verificationStatus || 'PENDING';
    return status === 'PENDING' || status === 'VERIFIED' || status === 'REJECTED';
  }

  onStatusBadgeClick(record: CustomerCylinderLedger): void {
    if (!record.id || !this.isStatusToggleAllowed(record)) {
      return;
    }
    const status = record.verificationStatus || 'PENDING';
    this.openActionModal(status === 'PENDING' ? 'VERIFY' : 'PENDING', record);
  }

  triggerRejectFromPending(): void {
    if (!this.actionTarget?.id) return;
    if (!this.actionRemark.trim()) {
      this.toastr.info('Remark is required to reject a transaction', 'Info');
      return;
    }
    this.ledgerService.rejectBankConfirmation(this.actionTarget.id, this.actionRemark.trim())
      .subscribe({
        next: () => {
          this.toastr.success('Transaction marked as rejected', 'Success');
          this.closeActionModal();
          this.loadQueue();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          this.toastr.error(err?.error?.message || err?.message || 'Rejection failed', 'Error');
        }
      });
  }

  private loadBankAccounts(): void {
    this.bankAccountService.getAllBankAccountsAll().subscribe({
      next: (accounts) => {
        this.bankAccounts = accounts || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.bankAccounts = [];
      }
    });
  }

  private loadPaymentModes(): void {
    this.paymentModeService.getAllPaymentModesAll().subscribe({
      next: (modes) => {
        this.paymentModes = modes || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.paymentModes = [];
      }
    });
  }

  private loadUsers(): void {
    this.userService.getUsers().subscribe({
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
