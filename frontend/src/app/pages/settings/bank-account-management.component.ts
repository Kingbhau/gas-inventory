import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEdit, faTrash, faPlus } from '@fortawesome/free-solid-svg-icons';
import { BankAccountService } from '../../services/bank-account.service';
import { BankAccount } from '../../models/bank-account.model';
import { PageResponse } from '../../models/page-response';

@Component({
  selector: 'app-bank-account-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    FontAwesomeModule
  ],
  templateUrl: './bank-account-management.component.html',
  styleUrl: './bank-account-management.component.css'
})
export class BankAccountManagementComponent implements OnInit, OnDestroy {
  bankAccountForm!: FormGroup;
  bankAccounts: BankAccount[] = [];

  isLoading = false;
  isSubmitting = false;
  showModal = false;
  editingBankAccountId: number | null = null;

  // Font Awesome icons
  faEdit = faEdit;
  faTrash = faTrash;
  faPlus = faPlus;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private bankAccountService: BankAccountService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadBankAccounts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize bank account form
   */
  private initForm(): void {
    this.bankAccountForm = this.fb.group({
      bankName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      accountNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(50)]],
      accountHolderName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      accountName: ['', [Validators.maxLength(100)]],
      accountType: [''],
      isActive: ['true']
    });
  }

  /**
   * Load all bank accounts
   */
  private loadBankAccounts(): void {
    this.isLoading = true;

    this.bankAccountService.getAllBankAccounts()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: PageResponse<BankAccount>) => {
          this.bankAccounts = response.items || [];
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        (error: unknown) => {
          this.toastr.error('Failed to load bank accounts');
          this.isLoading = false;
          this.cdr.markForCheck();
        }
      );
  }

  /**
   * Open modal for creating new bank account
   */
  openCreateModal(): void {
    this.editingBankAccountId = null;
    this.bankAccountForm.reset({ isActive: 'true', accountType: '' });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  /**
   * Open modal for editing bank account
   */
  openEditModal(bankAccount: BankAccount): void {
    this.editingBankAccountId = bankAccount.id || null;
    this.bankAccountForm.patchValue({
      bankName: bankAccount.bankName,
      accountNumber: bankAccount.accountNumber,
      accountHolderName: bankAccount.accountHolderName,
      accountName: bankAccount.accountName || '',
      accountType: bankAccount.accountType || '',
      isActive: (bankAccount.isActive !== false ? 'true' : 'false')
    });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  /**
   * Close modal
   */
  closeModal(): void {
    this.showModal = false;
    this.editingBankAccountId = null;
    this.bankAccountForm.reset();
    this.cdr.markForCheck();
  }

  /**
   * Save bank account (create or update)
   */
  saveBankAccount(): void {
    if (!this.bankAccountForm.valid) {
      this.toastr.error('Please fill all required fields');
      return;
    }

    this.isSubmitting = true;
    const formData = {
      ...this.bankAccountForm.value,
      isActive: this.bankAccountForm.value.isActive === 'true' || this.bankAccountForm.value.isActive === true
    };

    if (this.editingBankAccountId) {
      // Update existing
      this.bankAccountService.updateBankAccount(this.editingBankAccountId, formData)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          (response) => {
            this.toastr.success('Bank account updated successfully');
            this.closeModal();
            this.loadBankAccounts();
            this.isSubmitting = false;
            this.cdr.markForCheck();
          },
          (error) => {
            this.toastr.error(error.error?.message || 'Failed to update bank account');
            this.isSubmitting = false;
            this.cdr.markForCheck();
          }
        );
    } else {
      // Create new
      this.bankAccountService.createBankAccount(formData)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          (response) => {
            this.toastr.success('Bank account created successfully');
            this.closeModal();
            this.loadBankAccounts();
            this.isSubmitting = false;
            this.cdr.markForCheck();
          },
          (error) => {
            this.toastr.error(error.error?.message || 'Failed to create bank account');
            this.isSubmitting = false;
            this.cdr.markForCheck();
          }
        );
    }
  }

  /**
   * Delete bank account
   */
  deleteBankAccount(bankAccount: BankAccount): void {
    if (!confirm(`Are you sure you want to delete ${bankAccount.bankName}?`)) {
      return;
    }

    if (!bankAccount.id) return;

    this.bankAccountService.deleteBankAccount(bankAccount.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        () => {
          this.toastr.success('Bank account deleted successfully');
          this.loadBankAccounts();
          this.cdr.markForCheck();
        },
        (error) => {
          this.toastr.error('Failed to delete bank account');
          this.cdr.markForCheck();
        }
      );
  }

  /**
   * Format currency for display
   */
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

  /**
   * Get badge color based on status
   */
  getStatusColor(isActive: boolean | undefined): string {
    return isActive ? 'badge-active' : 'badge-inactive';
  }

  /**
   * Get status text
   */
  getStatusText(isActive: boolean | undefined): string {
    return isActive ? 'Active' : 'Inactive';
  }
}
