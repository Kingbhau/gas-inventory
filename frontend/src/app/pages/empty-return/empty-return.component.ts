import { catchError, of, finalize, Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList, ViewChild } from '@angular/core';
import { ReactiveFormsModule, FormArray } from '@angular/forms';
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
import { AuthService } from '../../services/auth.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { WarehouseService } from '../../services/warehouse.service';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { SharedModule } from '../../shared/shared.module';
import { Customer } from '../../models/customer.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { Warehouse } from '../../models/warehouse.model';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { InventoryStock } from '../../models/inventory-stock.model';
import { EmptyReturnRequest } from '../../models/empty-return-request.model';
import { PageResponse } from '../../models/page-response';

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
    @ViewChild('variantAutocomplete') variantAutocomplete?: AutocompleteInputComponent;
    
    private destroy$ = new Subject<void>();
    emptyReturnForm: FormGroup;
    submitting = false;
    customers: Customer[] = [];
    variants: CylinderVariant[] = [];
    filteredVariants: CylinderVariant[] = [];
    warehouses: Warehouse[] = [];
    bankAccounts: BankAccount[] = [];
    paymentModes: PaymentMode[] = [];
    successMessage = '';
    currentBalance: number | null = null;
    balanceLoading = false;
    balanceError = '';
    currentDue: number | null = null;
    dueLoading = false;
    dueError = '';
    isStaff = false;
    private customerSearch$ = new Subject<string>();
    showDuplicateConfirm = false;
    duplicateReturnInfo: {
        customerName: string;
        warehouseName: string;
        variantName: string;
        emptyQty: number;
        transactionDate: string;
        lastReference?: string;
    } | null = null;
    pendingEmptyReturnRequest: EmptyReturnRequest | null = null;

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
        private authService: AuthService,
        private cdr: ChangeDetectorRef
    ) {
        this.emptyReturnForm = this.fb.group({
            warehouseId: [null, Validators.required],
            customerId: [null, Validators.required],
            variantId: [null, Validators.required],
            emptyIn: [null, [Validators.required, Validators.min(1)]],
            amountReceived: [null],
            paymentType: ['SINGLE'],
            paymentMode: [''],
            bankAccountId: [null],
            paymentSplits: this.fb.array([]),
            transactionDate: [this.dateUtility.getTodayInIST(), Validators.required]
        });

        this.paymentSplits.clear();
        this.addPaymentSplit();
        this.setPaymentSplitsEnabled(false);

        // Add conditional validation for paymentMode
        this.emptyReturnForm.get('amountReceived')?.valueChanges.subscribe(() => {
            const modeControl = this.emptyReturnForm.get('paymentMode');
            const amountReceived = this.emptyReturnForm.get('amountReceived')?.value;
            
            if (!this.isMultiplePayment && amountReceived && amountReceived > 0) {
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

        this.emptyReturnForm.get('paymentType')?.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe((type: string) => {
                const amountControl = this.emptyReturnForm.get('amountReceived');
                const modeControl = this.emptyReturnForm.get('paymentMode');
                const bankControl = this.emptyReturnForm.get('bankAccountId');

                if (type === 'MULTIPLE') {
                    amountControl?.disable({ emitEvent: false });
                    modeControl?.clearValidators();
                    modeControl?.setValue('', { emitEvent: false });
                    bankControl?.clearValidators();
                    bankControl?.setValue(null, { emitEvent: false });
                    this.setPaymentSplitsEnabled(true);
                    this.syncAmountReceivedFromSplits();
                } else {
                    amountControl?.enable({ emitEvent: false });
                    this.setPaymentSplitsEnabled(false);
                }
                modeControl?.updateValueAndValidity({ emitEvent: false });
                bankControl?.updateValueAndValidity({ emitEvent: false });
                this.cdr.markForCheck();
            });

        // Load bank accounts on init
        this.loadBankAccounts();
    }

    loadBankAccounts() {
        this.bankAccountService.getActiveBankAccounts()
            .subscribe({
                next: (response: BankAccount[]) => {
                    this.bankAccounts = response || [];
                },
                error: (error: unknown) => {
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
                error: (error: unknown) => {
                    this.paymentModes = [];
                    this.cdr.markForCheck();
                }
            });
    }

    getSelectedPaymentMode(modeName: string): PaymentMode | undefined {
        return this.paymentModes.find(mode => mode.name === modeName);
    }

    get paymentSplits(): FormArray {
        return this.emptyReturnForm.get('paymentSplits') as FormArray;
    }

    get isMultiplePayment(): boolean {
        return this.emptyReturnForm.get('paymentType')?.value === 'MULTIPLE';
    }

    get amountReceivedForSummary(): number {
        return this.parseAmountInput(this.emptyReturnForm.get('amountReceived')?.value);
    }

    get projectedDueAfterPayment(): number | null {
        if (this.currentDue === null || this.currentDue === undefined) {
            return null;
        }
        return this.currentDue - this.amountReceivedForSummary;
    }

    private createPaymentSplitGroup(): FormGroup {
        return this.fb.group({
            modeOfPayment: [null, Validators.required],
            bankAccountId: [{ value: null, disabled: true }],
            amount: [null, [Validators.required, Validators.min(0.01)]],
            note: ['']
        });
    }

    addPaymentSplit(): void {
        const splitGroup = this.createPaymentSplitGroup();
        splitGroup.get('modeOfPayment')?.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe((mode: string | null) => {
                const bankAccountControl = splitGroup.get('bankAccountId');
                const selectedMode = mode ? this.getSelectedPaymentMode(mode) : undefined;
                if (selectedMode?.isBankAccountRequired) {
                    bankAccountControl?.enable({ emitEvent: false });
                    bankAccountControl?.setValidators(Validators.required);
                } else {
                    bankAccountControl?.clearValidators();
                    bankAccountControl?.setValue(null, { emitEvent: false });
                    bankAccountControl?.disable({ emitEvent: false });
                }
                bankAccountControl?.updateValueAndValidity({ emitEvent: false });
            });
        splitGroup.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => this.syncAmountReceivedFromSplits());
        this.paymentSplits.push(splitGroup);
        this.syncAmountReceivedFromSplits();
    }

    removePaymentSplit(index: number): void {
        if (this.paymentSplits.length <= 1) {
            return;
        }
        this.paymentSplits.removeAt(index);
        this.syncAmountReceivedFromSplits();
    }

    private parseAmountInput(value: unknown): number {
        if (typeof value === 'number') return value;
        if (typeof value === 'string') {
            const cleaned = value.replace(/,/g, '').trim();
            const parsed = parseFloat(cleaned);
            return isNaN(parsed) ? 0 : parsed;
        }
        return 0;
    }

    private syncAmountReceivedFromSplits(): void {
        if (!this.isMultiplePayment) {
            return;
        }
        const total = this.paymentSplits.controls.reduce((sum, control) => {
            const amount = this.parseAmountInput(control.get('amount')?.value);
            return sum + amount;
        }, 0);
        this.emptyReturnForm.get('amountReceived')?.setValue(total, { emitEvent: false });
        this.emptyReturnForm.get('amountReceived')?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
    }

    private setPaymentSplitsEnabled(enabled: boolean): void {
        if (enabled) {
            this.paymentSplits.enable({ emitEvent: false });
            if (this.paymentSplits.length === 0) {
                this.addPaymentSplit();
            }
            return;
        }
        this.paymentSplits.disable({ emitEvent: false });
    }

    get isSaveButtonDisabled(): boolean {
        if (!this.emptyReturnForm || this.emptyReturnForm.invalid || this.submitting) {
            return true;
        }

        if (this.isMultiplePayment) {
            const splits = this.paymentSplits.controls;
            if (!splits.length) {
                return true;
            }
            let splitTotal = 0;
            for (const control of splits) {
                const mode = control.get('modeOfPayment')?.value;
                const amount = this.parseAmountInput(control.get('amount')?.value);
                if (!mode || amount <= 0) {
                    return true;
                }
                const selectedMode = this.getSelectedPaymentMode(mode);
                if (selectedMode?.isBankAccountRequired && !control.get('bankAccountId')?.value) {
                    return true;
                }
                splitTotal += amount;
            }
            const amountReceived = this.parseAmountInput(this.emptyReturnForm.get('amountReceived')?.value);
            return Math.abs(splitTotal - amountReceived) > 0.009;
        }

        const amountReceived = this.parseAmountInput(this.emptyReturnForm.get('amountReceived')?.value);
        const modeOfPayment = this.emptyReturnForm.get('paymentMode')?.value;
        if (amountReceived > 0 && !modeOfPayment) {
            return true;
        }
        const selectedPaymentMode = this.getSelectedPaymentMode(modeOfPayment);
        return !!(modeOfPayment && selectedPaymentMode?.isBankAccountRequired && !this.emptyReturnForm.get('bankAccountId')?.value);
    }

    ngOnInit() {
        const role = this.authService.getUserInfo()?.role || '';
        this.isStaff = role === 'STAFF';
        this.applyStaffDateRestriction();
        this.loadingService.show('Loading customers and variants...');
        this.loadPaymentModes();
        this.customerSearch$
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
            .subscribe((term) => this.loadCustomers(term));
        this.onCustomerSearch('');
        const variantSub = this.variantService.getActiveVariants()
            .pipe(
                finalize(() => this.loadingService.hide()),
                catchError((err: unknown) => {
                    const errorObj = err as { error?: { message?: string }; message?: string };
                    const errorMessage = errorObj?.error?.message || errorObj?.message || 'Failed to load variants';
                    this.toastr.error(errorMessage, 'Error');
                    return of([] as CylinderVariant[]);
                })
            )
            .subscribe((data: CylinderVariant[]) => {
                this.variants = data || [];
                this.filteredVariants = this.variants;
                this.cdr.markForCheck();
            });

        // Load warehouses
        this.warehouseService.getActiveWarehouses()
            .pipe(
                catchError((err: unknown) => {
                    const errorObj = err as { error?: { message?: string }; message?: string };
                    const errorMessage = errorObj?.error?.message || errorObj?.message || 'Failed to load warehouses';
                    this.toastr.error(errorMessage, 'Error');
                    return of([] as Warehouse[]);
                })
            )
            .subscribe((data: Warehouse[]) => {
                this.warehouses = data || [];
                this.cdr.markForCheck();
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    onCustomerSearch(term: string) {
        this.customerSearch$.next(term || '');
    }

    private loadCustomers(search: string = '') {
        this.customerService.getActiveCustomersPaged(0, 20, 'name', 'ASC', search)
            .pipe(
                catchError((err: unknown) => {
                    const errorObj = err as { error?: { message?: string }; message?: string };
                    const errorMessage = errorObj?.error?.message || errorObj?.message || 'Failed to load customers';
                    this.toastr.error(errorMessage, 'Error');
                    return of({
                        items: [],
                        totalElements: 0,
                        totalPages: 1,
                        page: 0,
                        size: 20
                    } as PageResponse<Customer>);
                })
            )
            .subscribe((response: PageResponse<Customer>) => {
                this.customers = response.items || [];
                this.cdr.markForCheck();
            });
    }

    private applyStaffDateRestriction() {
        if (!this.isStaff) {
            return;
        }
        const today = this.dateUtility.getTodayInIST();
        this.emptyReturnForm.get('transactionDate')?.setValue(today, { emitEvent: false });
        this.emptyReturnForm.get('transactionDate')?.disable({ emitEvent: false });
    }

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
            paymentType: 'SINGLE',
            paymentMode: '',
            bankAccountId: null,
            transactionDate: this.dateUtility.getTodayInIST()
        });
        this.paymentSplits.clear();
        this.addPaymentSplit();
        this.setPaymentSplitsEnabled(false);

        this.emptyReturnForm.markAsPristine();
        this.emptyReturnForm.markAsUntouched();
        this.filteredVariants = this.variants;
        this.successMessage = '';
        this.currentBalance = null;
        this.balanceLoading = false;
        this.balanceError = '';
        this.currentDue = null;
        this.dueLoading = false;
        this.dueError = '';
        this.applyStaffDateRestriction();
        
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
    onWarehouseSelected(warehouse: Warehouse | null): void {
        if (warehouse && warehouse.id) {
            this.emptyReturnForm.get('warehouseId')?.setValue(warehouse.id);
        } else {
            this.emptyReturnForm.get('warehouseId')?.setValue(null);
        }
    }

    /**
     * Handle customer selection from autocomplete
     */
    onCustomerSelected(customer: Customer | null): void {
        if (customer && customer.id) {
            this.emptyReturnForm.get('customerId')?.setValue(customer.id);
            this.emptyReturnForm.get('customerId')?.markAsTouched();
            // Filter variants based on customer's configured variants
            this.filterVariantsByCustomerConfig(customer.id);
            // Clear variant selection when customer changes
            this.emptyReturnForm.get('variantId')?.setValue(null);
            this.variantAutocomplete?.resetInput();
            this.loadCurrentBalance();
            this.loadCurrentDue();
            this.cdr.markForCheck();
        } else {
            this.emptyReturnForm.get('customerId')?.setValue(null);
            this.variantAutocomplete?.resetInput();
            this.filteredVariants = this.variants;
            this.currentBalance = null;
            this.balanceLoading = false;
            this.balanceError = '';
            this.currentDue = null;
            this.dueLoading = false;
            this.dueError = '';
            this.cdr.markForCheck();
        }
    }

    /**
     * Handle variant selection from autocomplete
     */
    onVariantSelected(variant: CylinderVariant | null): void {
        if (variant && variant.id) {
            this.emptyReturnForm.get('variantId')?.setValue(variant.id);
            this.emptyReturnForm.get('variantId')?.markAsTouched();
            this.loadCurrentBalance();
        } else {
            this.emptyReturnForm.get('variantId')?.setValue(null);
            this.currentBalance = null;
            this.balanceLoading = false;
            this.balanceError = '';
        }
    }

    private loadCurrentBalance(): void {
        const customerId = this.emptyReturnForm.get('customerId')?.value;
        const variantId = this.emptyReturnForm.get('variantId')?.value;
        if (!customerId || !variantId) {
            this.currentBalance = null;
            this.balanceLoading = false;
            this.balanceError = '';
            return;
        }
        this.balanceLoading = true;
        this.balanceError = '';
        this.ledgerService.getBalance(customerId, variantId)
            .pipe(
                catchError((err: unknown) => {
                    this.balanceError = 'Current balance unavailable';
                    return of(null as number | null);
                }),
                finalize(() => {
                    this.balanceLoading = false;
                    this.cdr.markForCheck();
                }),
                takeUntil(this.destroy$)
            )
            .subscribe((balance: number | null) => {
                if (balance !== null && balance !== undefined) {
                    this.currentBalance = balance;
                } else {
                    this.currentBalance = null;
                }
            });
    }

    private loadCurrentDue(): void {
        const customerId = this.emptyReturnForm.get('customerId')?.value;
        if (!customerId) {
            this.currentDue = null;
            this.dueLoading = false;
            this.dueError = '';
            return;
        }
        this.dueLoading = true;
        this.dueError = '';
        this.ledgerService.getCustomerDueAmounts([customerId])
            .pipe(
                catchError((err: unknown) => {
                    this.dueError = 'Current due unavailable';
                    return of(null as Record<number, number> | null);
                }),
                finalize(() => {
                    this.dueLoading = false;
                    this.cdr.markForCheck();
                }),
                takeUntil(this.destroy$)
            )
            .subscribe((dueMap: Record<number, number> | null) => {
                if (dueMap && typeof dueMap[customerId] !== 'undefined') {
                    this.currentDue = dueMap[customerId];
                } else {
                    this.currentDue = 0;
                }
            });
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
        let configuredVariantIds: number[] = [];
        try {
            if (typeof customer.configuredVariants === 'string') {
                configuredVariantIds = JSON.parse(customer.configuredVariants);
            } else if (Array.isArray(customer.configuredVariants)) {
                configuredVariantIds = customer.configuredVariants;
            }
        } catch (e) {
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
        const filteredMap = new Map<number, CylinderVariant>();
        this.variants.forEach((v) => {
            if (typeof v.id !== 'number') {
                return;
            }
            const isActive = v.active !== false;
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
        
        const formData = this.emptyReturnForm.getRawValue();
        const paymentMode = formData.paymentMode;
        const bankAccountId = formData.bankAccountId;
        const paymentType = formData.paymentType || 'SINGLE';
        
        // Add bankAccountId to request if required by payment mode configuration
        const request: EmptyReturnRequest = {
            ...formData,
            amountReceived: this.parseAmountInput(formData.amountReceived),
            transactionDate: this.isStaff
                ? this.dateUtility.getTodayInIST()
                : (formData.transactionDate || this.dateUtility.getTodayInIST())
        };

        if (paymentType === 'MULTIPLE') {
            const paymentSplits = this.paymentSplits.controls
                .map((control) => ({
                    modeOfPayment: control.get('modeOfPayment')?.value,
                    bankAccountId: control.get('bankAccountId')?.value ? Number(control.get('bankAccountId')?.value) : undefined,
                    amount: this.parseAmountInput(control.get('amount')?.value),
                    note: control.get('note')?.value || undefined
                }))
                .filter((split) => split.modeOfPayment && split.amount > 0);

            if (paymentSplits.length === 0) {
                this.toastr.error('Add at least one valid payment split.', 'Validation Error');
                this.submitting = false;
                return;
            }

            for (const split of paymentSplits) {
                const selectedMode = this.getSelectedPaymentMode(split.modeOfPayment);
                if (selectedMode?.isBankAccountRequired && !split.bankAccountId) {
                    this.toastr.error(`Bank account is required for payment mode: ${split.modeOfPayment}`, 'Validation Error');
                    this.submitting = false;
                    return;
                }
            }

            const splitTotal = paymentSplits.reduce((sum, split) => sum + split.amount, 0);
            request.paymentType = 'MULTIPLE';
            request.paymentSplits = paymentSplits;
            request.amountReceived = splitTotal;
            request.paymentMode = undefined;
            request.bankAccountId = undefined;
        } else {
            const selectedMode = this.getSelectedPaymentMode(paymentMode);
            const isBankAccountRequired = selectedMode ? selectedMode.isBankAccountRequired : false;
            request.paymentType = 'SINGLE';
            request.paymentSplits = undefined;
            request.paymentMode = paymentMode || undefined;
            request.bankAccountId = paymentMode && isBankAccountRequired && bankAccountId ? Number(bankAccountId) : undefined;
        }
        
        this.checkDuplicateAndSubmit(request);
    }

    private getLatestEmptyReturnForCustomer(customerId: number) {
        return this.ledgerService.getEmptyReturns(0, 1, 'createdDate', 'DESC', undefined, undefined, customerId.toString())
            .pipe(
                catchError(() => of({
                    items: [],
                    totalElements: 0,
                    totalPages: 1,
                    page: 0,
                    size: 1
                } as PageResponse<CustomerCylinderLedger>))
            );
    }

    private isDuplicateEmptyReturn(lastEntry: CustomerCylinderLedger | null, request: EmptyReturnRequest): boolean {
        if (!lastEntry) return false;
        const sameCustomer = lastEntry.customerId === request.customerId;
        const sameWarehouse = (lastEntry.warehouseId ?? null) === (request.warehouseId ?? null);
        const sameVariant = lastEntry.variantId === request.variantId;
        const sameEmptyQty = Number(lastEntry.emptyIn || 0) === Number(request.emptyIn || 0);
        const sameDate = String(lastEntry.transactionDate || '') === String(request.transactionDate || '');
        return sameCustomer && sameWarehouse && sameVariant && sameEmptyQty && sameDate;
    }

    private buildDuplicateReturnInfo(lastEntry: CustomerCylinderLedger, request: EmptyReturnRequest) {
        const customer = this.customers.find(c => c.id === request.customerId);
        const warehouse = this.warehouses.find(w => w.id === request.warehouseId);
        const variant = this.variants.find(v => v.id === request.variantId);
        return {
            customerName: customer?.name || lastEntry.customerName || 'Customer',
            warehouseName: warehouse?.name || lastEntry.warehouseName || 'Warehouse',
            variantName: variant?.name || lastEntry.variantName || 'Variant',
            emptyQty: Number(request.emptyIn || 0),
            transactionDate: String(request.transactionDate || ''),
            lastReference: lastEntry.transactionReference
        };
    }

    private checkDuplicateAndSubmit(request: EmptyReturnRequest): void {
        if (!request.customerId) {
            this.createEmptyReturn(request);
            return;
        }
        this.getLatestEmptyReturnForCustomer(request.customerId)
            .subscribe((response: PageResponse<CustomerCylinderLedger>) => {
                const lastEntry = response.items && response.items.length > 0 ? response.items[0] : null;
                if (lastEntry && this.isDuplicateEmptyReturn(lastEntry, request)) {
                    this.pendingEmptyReturnRequest = request;
                    this.duplicateReturnInfo = this.buildDuplicateReturnInfo(lastEntry, request);
                    this.showDuplicateConfirm = true;
                    this.submitting = false;
                    this.cdr.markForCheck();
                    return;
                }
                this.createEmptyReturn(request);
            });
    }

    confirmDuplicateEmptyReturn(): void {
        if (!this.pendingEmptyReturnRequest) {
            this.showDuplicateConfirm = false;
            this.duplicateReturnInfo = null;
            return;
        }
        const request = this.pendingEmptyReturnRequest;
        this.pendingEmptyReturnRequest = null;
        this.showDuplicateConfirm = false;
        this.duplicateReturnInfo = null;
        this.createEmptyReturn(request);
    }

    cancelDuplicateEmptyReturn(): void {
        this.pendingEmptyReturnRequest = null;
        this.showDuplicateConfirm = false;
        this.duplicateReturnInfo = null;
        this.submitting = false;
        this.cdr.markForCheck();
    }

    private createEmptyReturn(request: EmptyReturnRequest): void {
        const idempotencyKey = this.generateIdempotencyKey();
        const sub = this.ledgerService.recordEmptyReturn(request, idempotencyKey)
            .pipe(
                finalize(() => {
                    this.submitting = false;
                    this.cdr.markForCheck();
                }),
                catchError((err: unknown) => {
                    const errorObj = err as { error?: { message?: string }; message?: string };
                    const errorMessage = errorObj?.error?.message || errorObj?.message || 'Failed to record return';
                    this.toastr.error(errorMessage, 'Error');
                    return of(null as CustomerCylinderLedger | null);
                })
            )
            .subscribe({
                next: (result: CustomerCylinderLedger | null) => {
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

    private generateIdempotencyKey(): string {
        if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
            return crypto.randomUUID();
        }
        return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    }
}
