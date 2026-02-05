import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPlus, faEdit, faTrash, faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { PaymentModeService } from '../../services/payment-mode.service';
import { PaymentMode } from '../../models/payment-mode.model';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-payment-mode-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FontAwesomeModule, SharedModule],
  templateUrl: './payment-mode-management.component.html',
  styleUrl: './payment-mode-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentModeManagementComponent implements OnInit, OnDestroy {
  paymentModes: PaymentMode[] = [];
  paymentModeForm!: FormGroup;
  isLoading = false;
  isSubmitting = false;
  showModal = false;
  editingId: number | null = null;

  // Icons
  faPlus = faPlus;
  faEdit = faEdit;
  faTrash = faTrash;
  faCheck = faCheck;
  faTimes = faTimes;

  constructor(
    private paymentModeService: PaymentModeService,
    private fb: FormBuilder,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadPaymentModes();
  }

  initForm() {
    this.paymentModeForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      code: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      isBankAccountRequired: [false],
      isActive: ['true']
    });
  }

  ngOnDestroy() {}

  loadPaymentModes() {
    this.isLoading = true;
    this.loadingService.show('Loading payment modes...');
    this.paymentModeService.getAllPaymentModesAll()
      .pipe(finalize(() => {
        this.loadingService.hide();
        this.isLoading = false;
      }))
      .subscribe({
        next: (response) => {
          this.paymentModes = response || [];
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error('Failed to load payment modes');
          this.cdr.markForCheck();
        }
      });
  }

  onAddNew() {
    this.editingId = null;
    this.paymentModeForm.reset({ isActive: 'true', isBankAccountRequired: false });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  onEdit(paymentMode: PaymentMode) {
    this.showModal = true;
    this.editingId = paymentMode.id || null;
    this.paymentModeForm.patchValue({
      name: paymentMode.name,
      code: paymentMode.code,
      description: paymentMode.description || '',
      isBankAccountRequired: paymentMode.isBankAccountRequired || false,
      isActive: (paymentMode.isActive !== false ? 'true' : 'false')
    });
    this.cdr.markForCheck();
  }

  closeModal() {
    this.showModal = false;
    this.editingId = null;
    this.paymentModeForm.reset();
    this.cdr.markForCheck();
  }

  onSubmit() {
    if (this.paymentModeForm.invalid) {
      this.toastr.error('Please fill all required fields');
      return;
    }

    this.isSubmitting = true;
    const formValue = {
      ...this.paymentModeForm.value,
      isActive: this.paymentModeForm.value.isActive === 'true' || this.paymentModeForm.value.isActive === true
    };

    if (this.editingId) {
      // Update existing
      this.paymentModeService.updatePaymentMode(this.editingId, formValue).subscribe({
        next: () => {
          this.toastr.success('Payment mode updated successfully');
          this.loadPaymentModes();
          this.closeModal();
          this.isSubmitting = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to update payment mode');
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      // Create new
      this.paymentModeService.createPaymentMode(formValue).subscribe({
        next: () => {
          this.toastr.success('Payment mode created successfully');
          this.loadPaymentModes();
          this.closeModal();
          this.isSubmitting = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to create payment mode');
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  onDelete(id: number | undefined) {
    if (!id) return;
    
    if (confirm('Are you sure you want to delete this payment mode?')) {
      this.paymentModeService.deletePaymentMode(id).subscribe({
        next: () => {
          this.toastr.success('Payment mode deleted successfully');
          this.loadPaymentModes();
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to delete payment mode');
          this.cdr.markForCheck();
        }
      });
    }
  }


}
