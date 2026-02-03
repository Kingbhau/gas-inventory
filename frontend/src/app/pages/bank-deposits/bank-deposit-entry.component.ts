import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCheckCircle, faTimes } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { Subject, takeUntil } from 'rxjs';

import { BankDepositService } from '../../services/bank-deposit.service';
import { BankAccountService } from '../../services/bank-account.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { LoadingService } from '../../services/loading.service';
import { SharedModule } from '../../shared/shared.module';
import { BankDeposit, BankAccount } from '../../models/index';
import { PaymentMode } from '../../models/payment-mode.model';

@Component({
  selector: 'app-bank-deposit-entry',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule, SharedModule],
  templateUrl: './bank-deposit-entry.component.html',
  styleUrl: './bank-deposit-entry.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BankDepositEntryComponent implements OnInit, OnDestroy {
  faCheckCircle = faCheckCircle;
  faTimes = faTimes;

  depositForm!: FormGroup;
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  isSubmitting = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private bankDepositService: BankDepositService,
    private bankAccountService: BankAccountService,
    private paymentModeService: PaymentModeService,
    private dateUtility: DateUtilityService,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadBankAccounts();
    this.loadPaymentModes();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize the form with default values
   */
  private initForm() {
    this.depositForm = this.fb.group({
      bankAccountId: [null, Validators.required],
      depositDate: [this.dateUtility.getTodayInIST(), Validators.required],
      depositAmount: [null, [Validators.required, Validators.min(1)]],
      referenceNumber: [''],
      paymentMode: ['', Validators.required],
      notes: ['']
    });
  }

  /**
   * Load available bank accounts
   */
  loadBankAccounts() {
    this.bankAccountService.getActiveBankAccounts()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading bank accounts:', error);
          this.toastr.error('Failed to load bank accounts');
          this.bankAccounts = [];
        }
      });
  }

  /**
   * Load payment modes
   */
  loadPaymentModes() {
    this.paymentModeService.getActivePaymentModes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PaymentMode[]) => {
          this.paymentModes = response || [];
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading payment modes:', error);
          this.toastr.error('Failed to load payment modes');
          this.paymentModes = [];
        }
      });
  }

  /**
   * Get selected bank account details
   */
  getSelectedBankAccount(): BankAccount | undefined {
    const accountId = this.depositForm.get('bankAccountId')?.value;
    return this.bankAccounts.find(ba => ba.id === accountId);
  }

  /**
   * Submit the deposit form
   */
  onSubmit() {
    if (this.depositForm.invalid) {
      this.toastr.error('Please fill all required fields correctly');
      return;
    }

    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.loadingService.show('Processing bank deposit...');

    const formData = this.depositForm.value;
    const deposit: BankDeposit = {
      bankAccountId: formData.bankAccountId,
      depositDate: formData.depositDate,
      depositAmount: parseFloat(formData.depositAmount),
      referenceNumber: formData.referenceNumber,
      paymentMode: formData.paymentMode,
      notes: formData.notes || ''
    };

    this.bankDepositService.createDeposit(deposit)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: BankDeposit) => {
          this.isSubmitting = false;
          this.loadingService.hide();
          this.toastr.success('Bank deposit recorded successfully!');
          this.resetForm();
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          this.isSubmitting = false;
          this.loadingService.hide();
          const errorMsg = error?.error?.message || 'Failed to record bank deposit';
          this.toastr.error(errorMsg);
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Reset the form to initial state
   */
  resetForm() {
    this.depositForm.reset({
      bankAccountId: null,
      depositDate: this.dateUtility.getTodayInIST(),
      depositAmount: null,
      referenceNumber: '',
      paymentMode: '',
      notes: ''
    });
  }

  /**
   * Check if form field is invalid and touched
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.depositForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /**
   * Get error message for a field
   */
  getFieldError(fieldName: string): string {
    const field = this.depositForm.get(fieldName);
    if (!field || !field.errors) {
      return '';
    }

    if (field.errors['required']) {
      return `${this.formatFieldName(fieldName)} is required`;
    }
    if (field.errors['minLength']) {
      return `${this.formatFieldName(fieldName)} must be at least ${field.errors['minLength'].requiredLength} characters`;
    }
    if (field.errors['min']) {
      return `${this.formatFieldName(fieldName)} must be greater than 0`;
    }
    return 'Invalid input';
  }

  /**
   * Format field name for display
   */
  private formatFieldName(fieldName: string): string {
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }
}
