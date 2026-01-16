import { catchError, of, finalize } from 'rxjs';


import { OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { ToastrService } from 'ngx-toastr';
import { LoadingService } from '../../services/loading.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { WarehouseService } from '../../services/warehouse.service';

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
    warehouses: any[] = [];
    successMessage = '';

    constructor(
        private fb: FormBuilder,
        private ledgerService: CustomerCylinderLedgerService,
        private toastr: ToastrService,
        private customerService: CustomerService,
        private variantService: CylinderVariantService,
        private loadingService: LoadingService,
        private warehouseService: WarehouseService
    ) {
        this.emptyReturnForm = this.fb.group({
            warehouseId: [null, Validators.required],
            customerId: [null, Validators.required],
            variantId: [null, Validators.required],
            emptyIn: [null, [Validators.required, Validators.min(1)]],
            amountReceived: [null],
            paymentMode: [''],
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
            .subscribe((data: any) => this.variants = data as any[]);

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
        } else {
            this.emptyReturnForm.get('customerId')?.setValue(null);
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
        const sub = this.ledgerService.recordEmptyReturn(this.emptyReturnForm.value)
            .pipe(
                catchError((err: any) => {
                    const errorMessage = err?.error?.message || err?.message || 'Failed to record return';
                    this.toastr.error(errorMessage, 'Error');
                    this.submitting = false;
                    return of(null);
                })
            )
            .subscribe({
                next: (result: any) => {
                    if (result) {
                        this.toastr.success('Empty cylinder return recorded');
                        this.resetForm();
                    }
                    this.submitting = false;
                },
                complete: () => {
                    sub.unsubscribe();
                }
            });
    }
}
