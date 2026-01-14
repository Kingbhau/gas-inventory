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
    successMessage = '';

    constructor(
        private fb: FormBuilder,
        private ledgerService: CustomerCylinderLedgerService,
        private toastr: ToastrService,
        private customerService: CustomerService,
        private variantService: CylinderVariantService,
        private loadingService: LoadingService
    ) {
        this.emptyReturnForm = this.fb.group({
            customerId: ['', Validators.required],
            variantId: ['', Validators.required],
            emptyIn: [null, [Validators.required, Validators.min(1)]],
            transactionDate: [new Date().toISOString().substring(0, 10), Validators.required]
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
    }

    ngOnDestroy() {}

    resetForm() {
        this.emptyReturnForm.reset({ emptyIn: null, transactionDate: new Date().toISOString().substring(0, 10) });
        this.emptyReturnForm.markAsPristine();
        this.emptyReturnForm.markAsUntouched();
    }

    submit() {
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
            .subscribe((result: any) => {
                if (result) {
                    // this.successMessage = 'Empty cylinder return recorded';
                    this.toastr.success('Empty cylinder return recorded');
                    this.resetForm();
                    // setTimeout(() => this.successMessage = '', 2500);
                }
                this.submitting = false;
                sub.unsubscribe();
            });
    }
}
