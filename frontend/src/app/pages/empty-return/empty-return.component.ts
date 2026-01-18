import { catchError, of, finalize } from 'rxjs';


import { OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { ToastrService } from 'ngx-toastr';
import { LoadingService } from '../../services/loading.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { WarehouseService } from '../../services/warehouse.service';
import { BankAccount } from '../../models/bank-account.model';

@Component({
    selector: 'app-empty-return',
    standalone: true,
    imports: [ReactiveFormsModule, CommonModule, AutocompleteInputComponent],
    templateUrl: './empty-return.component.html',
    styleUrls: ['./empty-return.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyReturnComponent implements OnInit, OnDestroy {
    emptyReturnForm: FormGroup;
    submitting = false;
    customers: any[] = [];
    variants: any[] = [];
    filteredVariants: any[] = [];
    warehouses: any[] = [];
    bankAccounts: BankAccount[] = [];
    successMessage = '';

    constructor(
        private fb: FormBuilder,
        private ledgerService: CustomerCylinderLedgerService,
        private bankAccountService: BankAccountService,
        private toastr: ToastrService,
        private customerService: CustomerService,
        private variantService: CylinderVariantService,
        private loadingService: LoadingService,
        private warehouseService: WarehouseService,
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
            transactionDate: [new Date().toISOString().substring(0, 10), Validators.required]
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
        });

        // When paymentMode changes, show/hide bank account field
        this.emptyReturnForm.get('paymentMode')?.valueChanges.subscribe((mode) => {
            const bankAccountControl = this.emptyReturnForm.get('bankAccountId');
            if (mode && mode.toUpperCase() !== 'CASH') {
                bankAccountControl?.setValidators(Validators.required);
            } else {
                bankAccountControl?.clearValidators();
                bankAccountControl?.reset();
            }
            bankAccountControl?.updateValueAndValidity();
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

    ngOnInit() {
        this.loadingService.show('Loading customers and variants...');
        const customerSub = this.customerService.getActiveCustomers()
            .pipe(
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to load customers';
                    this.toastr.error(errorMessage, 'Error');
                    return of([]);
                })
            )
            .subscribe((data: any) => this.customers = data as any[]);
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
            .subscribe((response: any) => {
                if (response.success) {
                    this.warehouses = response.data || [];
                }
            });
    }

    ngOnDestroy() {}

    resetForm() {
        this.emptyReturnForm.reset({ 
            warehouseId: null,
            customerId: null,
            variantId: null,
            emptyIn: null,
            amountReceived: null,
            paymentMode: '',
            bankAccountId: null,
            transactionDate: new Date().toISOString().substring(0, 10)
        });
        this.emptyReturnForm.markAsPristine();
        this.emptyReturnForm.markAsUntouched();
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
        } else {
            this.emptyReturnForm.get('customerId')?.setValue(null);
            this.filteredVariants = this.variants;
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
     * Filter variants based on customer's configured variants
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
        
        // Filter variants to only show those configured for this customer
        this.filteredVariants = this.variants.filter(v => 
            configuredVariantIds.includes(v.id)
        );
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
        
        // Add bankAccountId to request if payment is not CASH
        const request: any = {
            ...formData
        };
        
        if (paymentMode && paymentMode.toUpperCase() !== 'CASH' && bankAccountId) {
            request.bankAccountId = bankAccountId;
        }
        
        const sub = this.ledgerService.recordEmptyReturn(request)
            .pipe(
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to record return';
                    this.toastr.error(errorMessage, 'Error');
                    this.submitting = false;
                    this.cdr.markForCheck();
                    return of(null);
                })
            )
            .subscribe({
                next: (result: any) => {
                    if (result) {
                        const reference = result?.transactionReference || 'N/A';
                        this.toastr.success(`Empty cylinder return recorded - Reference: ${reference}`, 'Success');
                        this.resetForm();
                    }
                    this.submitting = false;
                    this.cdr.markForCheck();
                },
                complete: () => {
                    sub.unsubscribe();
                }
            });
    }
}
