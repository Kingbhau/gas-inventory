import { catchError, of, finalize } from 'rxjs';
import { OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { ToastrService } from 'ngx-toastr';
import { LoadingService } from '../../services/loading.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { WarehouseService } from '../../services/warehouse.service';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { SharedModule } from '../../shared/shared.module';

@Component({
    selector: 'app-empty-return',
    standalone: true,
    imports: [ReactiveFormsModule, CommonModule, AutocompleteInputComponent, SharedModule],
    templateUrl: './empty-return.component.html',
    styleUrls: ['./empty-return.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyReturnComponent implements OnInit, OnDestroy {
    @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
    
    emptyReturnForm: FormGroup;
    submitting = false;
    customers: any[] = [];
    variants: any[] = [];
    filteredVariants: any[] = [];
    warehouses: any[] = [];
    bankAccounts: BankAccount[] = [];
    paymentModes: PaymentMode[] = [];
    successMessage = '';

    constructor(
        private fb: FormBuilder,
        private ledgerService: CustomerCylinderLedgerService,
        private bankAccountService: BankAccountService,
        private paymentModeService: PaymentModeService,
        private toastr: ToastrService,
        private customerService: CustomerService,
        private variantService: CylinderVariantService,
        private loadingService: LoadingService,
        private warehouseService: WarehouseService,
        private dateUtility: DateUtilityService,
        private cdr: ChangeDetectorRef
    ) {
        this.emptyReturnForm = this.fb.group({
            warehouseId: [null, Validators.required],
            customerId: [null, Validators.required],
            variantId: [null, Validators.required],
            emptyIn: [null, [Validators.required, Validators.min(1)]],
            amountReceived: [null],
            paymentMode: [''],
            bankAccountId: [null],
            transactionDate: [this.dateUtility.getTodayInIST(), Validators.required]
        });

        // Add conditional validation for paymentMode
        this.emptyReturnForm.get('amountReceived')?.valueChanges.subscribe(() => {
            const modeControl = this.emptyReturnForm.get('paymentMode');
            const amountReceived = this.emptyReturnForm.get('amountReceived')?.value;
            
            if (amountReceived && amountReceived > 0) {
                modeControl?.setValidators(Validators.required);
            } else {
                modeControl?.clearValidators();
            }
            modeControl?.updateValueAndValidity();
            this.emptyReturnForm.updateValueAndValidity();
        });

        // When paymentMode changes, show/hide bank account field
        this.emptyReturnForm.get('paymentMode')?.valueChanges.subscribe((mode) => {
            const bankAccountControl = this.emptyReturnForm.get('bankAccountId');
            const selectedMode = this.getSelectedPaymentMode(mode);
            const isBankAccountRequired = selectedMode ? selectedMode.isBankAccountRequired : false;
            
            if (mode && isBankAccountRequired) {
                bankAccountControl?.setValidators(Validators.required);
            } else {
                bankAccountControl?.clearValidators();
                bankAccountControl?.reset();
            }
            bankAccountControl?.updateValueAndValidity();
            this.emptyReturnForm.updateValueAndValidity();
        });

        // Load bank accounts on init
        this.loadBankAccounts();
    }

    loadBankAccounts() {
        this.bankAccountService.getActiveBankAccounts()
            .subscribe({
                next: (response: any) => {
                    this.bankAccounts = response || [];
                },
                error: (error: any) => {
                    console.error('Error loading bank accounts:', error);
                    this.bankAccounts = [];
                }
            });
    }

    loadPaymentModes() {
        this.paymentModeService.getActivePaymentModes()
            .subscribe({
                next: (response: PaymentMode[]) => {
                    this.paymentModes = response || [];
                    this.cdr.markForCheck();
                },
                error: (error: any) => {
                    console.error('Error loading payment modes:', error);
                    this.paymentModes = [];
                    this.cdr.markForCheck();
                }
            });
    }

    getSelectedPaymentMode(modeName: string): PaymentMode | undefined {
        return this.paymentModes.find(mode => mode.name === modeName);
    }

    ngOnInit() {
        this.loadingService.show('Loading customers and variants...');
        this.loadPaymentModes();
        const customerSub = this.customerService.getActiveCustomers()
            .pipe(
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to load customers';
                    this.toastr.error(errorMessage, 'Error');
                    return of([]);
                })
            )
            .subscribe((data: any) => {
                this.customers = data as any[];
                this.cdr.markForCheck();
            });
        const variantSub = this.variantService.getActiveVariants()
            .pipe(
                finalize(() => this.loadingService.hide()),
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to load variants';
                    this.toastr.error(errorMessage, 'Error');
                    return of([]);
                })
            )
            .subscribe((data: any) => {
                this.variants = data as any[];
                this.filteredVariants = this.variants;
                this.cdr.markForCheck();
            });

        // Load warehouses
        this.warehouseService.getActiveWarehouses()
            .pipe(
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to load warehouses';
                    this.toastr.error(errorMessage, 'Error');
                    return of([]);
                })
            )
            .subscribe((data: any) => {
                this.warehouses = data || [];
                this.cdr.markForCheck();
            });
    }

    ngOnDestroy() {}

    resetForm() {
        // Reset autocomplete components
        if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
            this.autocompleteInputs.forEach(input => {
                if (input.resetInput) {
                    input.resetInput();
                }
            });
        }

        // Reset form completely
        this.emptyReturnForm.reset();
        
        // Reset with default values
        this.emptyReturnForm.patchValue({
            warehouseId: null,
            customerId: null,
            variantId: null,
            emptyIn: null,
            amountReceived: null,
            paymentMode: '',
            bankAccountId: null,
            transactionDate: this.dateUtility.getTodayInIST()
        });

        this.emptyReturnForm.markAsPristine();
        this.emptyReturnForm.markAsUntouched();
        this.filteredVariants = this.variants;
        this.successMessage = '';
        
        // Clear validation state on all controls
        Object.keys(this.emptyReturnForm.controls).forEach(key => {
            const control = this.emptyReturnForm.get(key);
            if (control) {
                control.markAsPristine();
                control.markAsUntouched();
            }
        });
        
        this.cdr.markForCheck();
    }

    /**
     * Handle warehouse selection from autocomplete
     */
    onWarehouseSelected(warehouse: any): void {
        if (warehouse && warehouse.id) {
            this.emptyReturnForm.get('warehouseId')?.setValue(warehouse.id);
        } else {
            this.emptyReturnForm.get('warehouseId')?.setValue(null);
        }
    }

    /**
     * Handle customer selection from autocomplete
     */
    onCustomerSelected(customer: any): void {
        if (customer && customer.id) {
            this.emptyReturnForm.get('customerId')?.setValue(customer.id);
            this.emptyReturnForm.get('customerId')?.markAsTouched();
            // Filter variants based on customer's configured variants
            this.filterVariantsByCustomerConfig(customer.id);
            // Clear variant selection when customer changes
            this.emptyReturnForm.get('variantId')?.setValue(null);
            this.cdr.markForCheck();
        } else {
            this.emptyReturnForm.get('customerId')?.setValue(null);
            this.filteredVariants = this.variants;
            this.cdr.markForCheck();
        }
    }

    /**
     * Handle variant selection from autocomplete
     */
    onVariantSelected(variant: any): void {
        if (variant && variant.id) {
            this.emptyReturnForm.get('variantId')?.setValue(variant.id);
            this.emptyReturnForm.get('variantId')?.markAsTouched();
        } else {
            this.emptyReturnForm.get('variantId')?.setValue(null);
        }
    }

    /**
     * Filter variants based on customer's configured variants AND active status
     */
    filterVariantsByCustomerConfig(customerId: number | null) {
        if (!customerId) {
            this.filteredVariants = this.variants;
            return;
        }
        
        // Find the customer and get their configured variants
        const customer = this.customers.find(c => c.id === customerId);
        if (!customer || !customer.configuredVariants) {
            this.filteredVariants = this.variants;
            return;
        }
        
        // Parse configuredVariants if it's a JSON string, otherwise treat as array
        let configuredVariantIds: any[] = [];
        try {
            if (typeof customer.configuredVariants === 'string') {
                configuredVariantIds = JSON.parse(customer.configuredVariants);
            } else if (Array.isArray(customer.configuredVariants)) {
                configuredVariantIds = customer.configuredVariants;
            }
        } catch (e) {
            console.error('Error parsing configuredVariants:', e);
            this.filteredVariants = this.variants;
            return;
        }
        
        if (!configuredVariantIds || configuredVariantIds.length === 0) {
            this.filteredVariants = this.variants;
            return;
        }
        
        // Remove duplicate IDs from configuredVariantIds
        const uniqueVariantIds = [...new Set(configuredVariantIds)];
        
        // Filter variants: must be configured for this customer
        // If variant has status property, check if ACTIVE, otherwise assume it's active (came from getActiveVariants)
        const filteredMap = new Map();
        this.variants.forEach(v => {
            const isActive = v.status ? v.status === 'ACTIVE' : true; // Assume active if no status property
            if (isActive && uniqueVariantIds.includes(v.id)) {
                if (!filteredMap.has(v.id)) {
                    filteredMap.set(v.id, v);
                }
            }
        });
        
        this.filteredVariants = Array.from(filteredMap.values());
    }

    submit() {
        // Validate warehouse is selected
        if (!this.emptyReturnForm.get('warehouseId')?.value) {
            this.toastr.error('Please select a warehouse', 'Validation Error');
            this.emptyReturnForm.get('warehouseId')?.markAsTouched();
            return;
        }

        if (this.emptyReturnForm.invalid) {
            this.emptyReturnForm.markAllAsTouched();
            this.toastr.error('Please correct the errors in the form.', 'Validation Error');
            return;
        }
        // Ensure emptyIn is always positive integer
        const emptyIn = Number(this.emptyReturnForm.get('emptyIn')?.value);
        if (isNaN(emptyIn) || emptyIn < 1) {
            this.toastr.error('Returned empty cylinders must be at least 1.', 'Validation Error');
            return;
        }
        this.submitting = true;
        
        const formData = this.emptyReturnForm.value;
        const paymentMode = formData.paymentMode;
        const bankAccountId = formData.bankAccountId;
        
        // Add bankAccountId to request if required by payment mode configuration
        const request: any = {
            ...formData
        };
        
        const selectedMode = this.getSelectedPaymentMode(paymentMode);
        const isBankAccountRequired = selectedMode ? selectedMode.isBankAccountRequired : false;
        
        if (paymentMode && isBankAccountRequired && bankAccountId) {
            request.bankAccountId = bankAccountId;
        }
        
        const sub = this.ledgerService.recordEmptyReturn(request)
            .pipe(
                finalize(() => {
                    this.submitting = false;
                    this.cdr.markForCheck();
                }),
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to record return';
                    this.toastr.error(errorMessage, 'Error');
                    return of(null);
                })
            )
            .subscribe({
                next: (result: any) => {
                    if (result) {
                        const reference = result?.transactionReference || 'N/A';
                        this.toastr.success(`Empty cylinder return recorded - Reference: ${reference}`, 'Success');
                        // Reset form after successful submission
                        this.resetForm();
                        this.filteredVariants = this.variants;
                    }
                }
            });
    }
}
