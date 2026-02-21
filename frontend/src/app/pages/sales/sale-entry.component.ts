import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule, FormControl } from '@angular/forms';
import { Component, OnInit, ViewChild, ViewChildren, QueryList, ChangeDetectionStrategy, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { startWith, map, finalize, debounceTime, distinctUntilChanged, takeUntil, forkJoin } from 'rxjs';
import { catchError, of, Subject } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { SharedModule } from '../../shared/shared.module';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { SaleService } from '../../services/sale.service';
import { BankAccountService } from '../../services/bank-account.service';
import { DataRefreshService } from '../../services/data-refresh.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-balance.service';
import { MonthlyPriceService } from '../../services/monthly-price.service';
import { CustomerVariantPriceService } from '../../services/customer-variant-price.service';
import { LoadingService } from '../../services/loading.service';
import { WarehouseService } from '../../services/warehouse.service';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentModeService } from '../../services/payment-mode.service';
import { PaymentMode } from '../../models/payment-mode.model';
import { DateUtilityService } from '../../services/date-utility.service';
import { AuthService } from '../../services/auth.service';
import { Customer } from '../../models/customer.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { Warehouse } from '../../models/warehouse.model';
import { CreateSaleRequest, SaleItemRequest } from '../../models/create-sale-request.model';
import { CustomerVariantPrice } from '../../models/customer-variant-price.model';
import { Sale } from '../../models/sale.model';

@Component({
  selector: 'app-sale-entry',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule, MatOptionModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './sale-entry.component.html',
  styleUrl: './sale-entry.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaleEntryComponent implements OnInit, OnDestroy {
  @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
  @ViewChild('variantAutocomplete') variantAutocomplete?: AutocompleteInputComponent;
  
  private destroy$ = new Subject<void>();
  saleForm!: FormGroup;
  successMessage = '';
  baseAmount = 0;
  discountPrice: number = 0; // Customer-specific discount price
  currentBalance: number | null = null;
  balanceLoading = false;
  balanceError = '';
  currentDue: number | null = null;
  dueLoading = false;
  dueError = '';

  customerIdControl = new FormControl(null, Validators.required);
  variantIdControl = new FormControl(null, Validators.required);
  customerDropdownConfig: Record<string, string | number | boolean | undefined | null> = {
    displayKey: 'name',
    search: true,
    placeholder: 'Select a customer',
    noResultsFound: 'No customer found',
    searchPlaceholder: 'Search customer...',
    height: '200px',
    customComparator: undefined,
    limitTo: 100
  };
  variantDropdownConfig: Record<string, string | number | boolean | undefined | null> = {
    displayKey: 'name',
    search: true,
    placeholder: 'Select variant',
    noResultsFound: 'No variant found',
    searchPlaceholder: 'Search variant...',
    height: '200px',
    customComparator: undefined,
    limitTo: 100
  };



  // Font Awesome Icons
  faCheckCircle = faCheckCircle;

  customers: Customer[] = [];
  variants: CylinderVariant[] = [];
  warehouses: Warehouse[] = [];
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  filteredCustomers: Customer[] = [];
  filteredVariants: CylinderVariant[] = [];
  customerSearch: string = '';
  variantSearch: string = '';
  isStaff = false;
  // filteredCustomers: any[] = [];
  // filteredVariants: any[] = [];
  // customerAutocompleteCtrl: FormControl = new FormControl('');
  // variantAutocompleteCtrl: FormControl = new FormControl('');

  // No need for display maps when storing objects

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private variantService: CylinderVariantService,
    private saleService: SaleService,
    private bankAccountService: BankAccountService,
    private monthlyPriceService: MonthlyPriceService,
    private variantPriceService: CustomerVariantPriceService,
    private toastr: ToastrService,
    private customerCylinderLedgerService: CustomerCylinderLedgerService,
    private loadingService: LoadingService,
    private warehouseService: WarehouseService,
    private dataRefreshService: DataRefreshService,
    private paymentModeService: PaymentModeService,
    private dateUtility: DateUtilityService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    const role = this.authService.getUserInfo()?.role || '';
    this.isStaff = role === 'STAFF';
    this.applyStaffDateRestriction();
    // OPTIMIZATION: Load all reference data in parallel instead of sequentially
    this.loadReferenceDataInParallel();
    
    // IMPORTANT: Listen for customer updates (when variants are reconfigured in customer management)
    this.dataRefreshService.customerUpdated$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.customerService.getActiveCustomers()
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (customers) => {
              this.customers = customers || [];
              this.filteredCustomers = this.customers;
              
              // Get current selections
              const currentCustomerId = this.saleForm.get('customerId')?.value?.id;
              const currentVariantId = this.saleForm.get('variantId')?.value?.id;
              
              // If a customer is currently selected, re-filter variants
              if (currentCustomerId) {
                const updatedCustomer = this.customers.find(c => c.id === currentCustomerId);
                if (updatedCustomer) {
                  // Update the selected customer object in the form with fresh data
                  this.saleForm.patchValue({ customerId: updatedCustomer }, { emitEvent: false });
                  
                  // Re-filter variants based on updated customer config
                  this.filterVariantsByCustomerConfig(currentCustomerId);
                  
                  // If the previously selected variant is no longer configured, clear it
                  if (currentVariantId && this.filteredVariants && this.filteredVariants.length > 0) {
                    const variantStillAvailable = this.filteredVariants.some(v => v.id === currentVariantId);
                    if (!variantStillAvailable) {
                      this.saleForm.patchValue({ variantId: null }, { emitEvent: false });
                    }
                  } else if (!currentVariantId || !this.filteredVariants || this.filteredVariants.length === 0) {
                    // Clear variant if no variants are now available
                    this.saleForm.patchValue({ variantId: null }, { emitEvent: false });
                  }
                }
              }
              
              this.cdr.markForCheck();
            },
            error: (err) => {
            }
          });
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private applyStaffDateRestriction() {
    if (!this.isStaff) {
      return;
    }
    const today = this.dateUtility.getTodayInIST();
    this.saleForm.get('saleDate')?.setValue(today, { emitEvent: false });
    this.saleForm.get('saleDate')?.disable({ emitEvent: false });
  }

  /**
   * Load all reference data in parallel for 3-4x faster page load
   * Before: Sequential loading (800-1000ms) 
   * After: Parallel loading (200-300ms)
   */
  private loadReferenceDataInParallel(): void {
    this.loadingService.show('Loading form data...');
    forkJoin([
      this.customerService.getActiveCustomers(),
      this.variantService.getActiveVariants(),
      this.warehouseService.getActiveWarehouses(),
      this.paymentModeService.getActivePaymentModes(),
      this.bankAccountService.getActiveBankAccounts()
    ])
    .pipe(
      finalize(() => this.loadingService.hide()),
      takeUntil(this.destroy$)
    )
    .subscribe({
      next: ([customers, variants, warehouses, paymentModes, bankAccounts]) => {
        this.customers = customers || [];
        this.variants = variants || [];
        this.warehouses = warehouses || [];
        this.paymentModes = paymentModes || [];
        this.bankAccounts = bankAccounts || [];
        this.filteredCustomers = this.customers;
        this.filteredVariants = this.variants;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toastr.error('Failed to load form data');
      }
    });

    // OPTIMIZATION: Debounce customer selection changes (300ms)
    this.saleForm.get('customerId')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => prev?.id === curr?.id),
        takeUntil(this.destroy$)
      )
      .subscribe(customerObj => {
        const customerId = customerObj && customerObj.id ? customerObj.id : null;
        this.filterVariantsByCustomerConfig(customerId);
        const variantObj = this.saleForm.get('variantId')?.value;
        if (variantObj && variantObj.id) {
          this.prefillPrice(customerId, variantObj.id);
        }
        this.loadCurrentBalance();
        this.loadCurrentDue();
        this.cdr.markForCheck();
      });
    
    // OPTIMIZATION: Debounce variant selection changes (300ms)
    this.saleForm.get('variantId')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => prev?.id === curr?.id),
        takeUntil(this.destroy$)
      )
      .subscribe(variantObj => {
        const variantId = variantObj && variantObj.id ? variantObj.id : null;
        const customerObj = this.saleForm.get('customerId')?.value;
        const customerId = customerObj && customerObj.id ? customerObj.id : null;
        this.prefillPrice(customerId, variantId);
        this.loadCurrentBalance();
        this.cdr.markForCheck();
      });

    // OPTIMIZATION: Debounce amount received changes (300ms) 
    this.saleForm.get('amountReceived')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        const modeControl = this.saleForm.get('modeOfPayment');
        const bankAccountControl = this.saleForm.get('bankAccountId');
        const amountReceived = this.saleForm.get('amountReceived')?.value;
        this.updatePaymentModeValidators(amountReceived > 0);
        this.cdr.markForCheck();
      });

    // OPTIMIZATION: Debounce payment mode changes (300ms)
    this.saleForm.get('modeOfPayment')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((mode) => {
        const bankAccountControl = this.saleForm.get('bankAccountId');
        if (mode === 'CHEQUE' || mode === 'BANK_TRANSFER') {
          bankAccountControl?.setValidators(Validators.required);
        } else {
          bankAccountControl?.clearValidators();
        }
        bankAccountControl?.updateValueAndValidity();
        this.cdr.markForCheck();
      });
  }

  displayCustomerName(customer: Customer | null): string {
    return customer && typeof customer === 'object' ? customer.name : '';
  }

  displayVariantName(variant: CylinderVariant | null): string {
    return variant && typeof variant === 'object' ? variant.name : '';
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
    if (!customer) {
      this.filteredVariants = this.variants;
      return;
    }

    if (!customer.configuredVariants) {
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
    
    // Filter variants: must be configured for this customer and active
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

  /**
   * Prefill price based on customer-specific pricing or monthly pricing
   * Priority: Customer-specific pricing > Monthly pricing
   */
  prefillPrice(customerId: number | null, variantId: number | null) {
    if (!customerId || !variantId) {
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
      return;
    }

    // First try to get customer-specific variant pricing
    this.variantPriceService.getPriceByVariant(customerId, variantId).subscribe({
      next: (response: CustomerVariantPrice | null) => {
        if (response) {
          // Use customer-specific sale price and discount price
          this.saleForm.get('basePrice')?.setValue(response.salePrice);
          this.discountPrice = response.discountPrice || 0;
          this.saleForm.get('basePrice')?.disable();
          this.calculateTotal();
        } else {
          // Fall back to monthly pricing
          this.prefillBasePrice(variantId);
        }
      },
      error: () => {
        // Fall back to monthly pricing if customer pricing not found
        this.discountPrice = 0;
        this.prefillBasePrice(variantId);
      }
    });
  }

  prefillBasePrice(variantId: number) {
    if (!variantId) {
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
      return;
    }
    const now = new Date();
    const monthYear = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}`;
    this.monthlyPriceService.getLatestPriceForMonth(variantId, monthYear).subscribe({
      next: (price) => {
        this.saleForm.get('basePrice')?.setValue(price.basePrice);
        this.discountPrice = 0; // Reset discount price for monthly pricing
        this.saleForm.get('basePrice')?.disable();
        this.calculateTotal();
      },
      error: () => {
        this.saleForm.get('basePrice')?.setValue(0);
        this.discountPrice = 0;
        this.saleForm.get('basePrice')?.disable();
        this.calculateTotal();
      }
    });
  }

  loadCustomers() {
    // Removed - now using parallel loading in loadReferenceDataInParallel()
  }

  loadVariants() {
    // Removed - now using parallel loading in loadReferenceDataInParallel()
  }

  loadWarehouses() {
    // Removed - now using parallel loading in loadReferenceDataInParallel()
  }

  loadBankAccounts() {
    // Removed - now using parallel loading in loadReferenceDataInParallel()
  }

  loadPaymentModes() {
    // Removed - now using parallel loading in loadReferenceDataInParallel()
  }

  getSelectedPaymentMode(modeName: string): PaymentMode | undefined {
    return this.paymentModes.find(mode => mode.name === modeName);
  }

  initForm() {
    this.saleForm = this.fb.group({
      warehouseId: [null, Validators.required],
      customerId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledIssuedQty: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      emptyReceivedQty: [null, [Validators.min(0), Validators.max(100)]],
      basePrice: [null, [Validators.required, Validators.min(0), Validators.max(10000)]],
      amountReceived: [null, [Validators.max(100000)]],
      modeOfPayment: [null],
      bankAccountId: [null],
      saleDate: [this.dateUtility.getTodayInIST(), Validators.required]
    });

    // Add conditional validation for modeOfPayment and bankAccountId
    this.saleForm.get('amountReceived')?.valueChanges.subscribe(() => {
      const modeControl = this.saleForm.get('modeOfPayment');
      const bankAccountControl = this.saleForm.get('bankAccountId');
      const amountReceived = this.saleForm.get('amountReceived')?.value;
      
      if (amountReceived && amountReceived > 0) {
        modeControl?.setValidators(Validators.required);
      } else {
        modeControl?.clearValidators();
      }
      modeControl?.updateValueAndValidity();
      this.saleForm.updateValueAndValidity();
    });

    // When modeOfPayment changes, show/hide bank account field
    this.saleForm.get('modeOfPayment')?.valueChanges.subscribe((mode) => {
      const bankAccountControl = this.saleForm.get('bankAccountId');
      const selectedMode = this.getSelectedPaymentMode(mode);
      if (mode && selectedMode?.isBankAccountRequired) {
        bankAccountControl?.setValidators(Validators.required);
      } else {
        bankAccountControl?.clearValidators();
        bankAccountControl?.reset();
      }
      bankAccountControl?.updateValueAndValidity();
      this.saleForm.updateValueAndValidity();
    });

    // Load bank accounts on init
    this.loadBankAccounts();
  }

  calculateTotal() {
    const filledQty = Number(this.saleForm.get('filledIssuedQty')?.value) || 0;
    const basePrice = Number(this.saleForm.get('basePrice')?.value) || 0;
    // Base amount calculated on actual base price
    this.baseAmount = Math.max(0, filledQty) * Math.max(0, basePrice);
    // Prevent negative baseAmount
    if (this.baseAmount < 0 || isNaN(this.baseAmount)) this.baseAmount = 0;
  }

  get totalAmount(): number {
    const filledQty = Number(this.saleForm.get('filledIssuedQty')?.value) || 0;
    const basePrice = Number(this.saleForm.get('basePrice')?.value) || 0;
    const discountAmount = this.discountPrice || 0;
    
    // Price per cylinder = Base Price - Discount Amount
    const pricePerCylinder = Math.max(0, basePrice - discountAmount);
    
    // Total amount = Quantity × Price Per Cylinder
    let total = Math.max(0, filledQty) * Math.max(0, pricePerCylinder);
    if (total < 0 || isNaN(total)) total = 0;
    return total;
  }

  get discountAmount(): number {
    const filledQty = Number(this.saleForm.get('filledIssuedQty')?.value) || 0;
    const discountValue = this.discountPrice || 0;
    
    // Total discount = Discount Per Unit × Quantity
    const discount = Math.max(0, filledQty) * Math.max(0, discountValue);
    if (discount < 0 || isNaN(discount)) return 0;
    return discount;
  }

  /**
   * Handle warehouse selection from autocomplete
   */
  onWarehouseSelected(warehouse: Warehouse | null): void {
    if (warehouse && warehouse.id) {
      // Set the entire warehouse object for consistency
      this.saleForm.get('warehouseId')?.setValue(warehouse, { emitEvent: true });
    } else {
      this.saleForm.get('warehouseId')?.setValue(null);
    }
  }

  onCustomerSelected(customer: Customer | null): void {
    if (customer && customer.id) {
      // Set the entire customer object (not just ID) so valueChanges subscription receives the full object
      this.saleForm.get('customerId')?.setValue(customer, { emitEvent: true });
      this.saleForm.get('customerId')?.markAsTouched();
      // Filter variants based on customer's configured variants
      this.filterVariantsByCustomerConfig(customer.id);
      // Clear variant selection when customer changes
      this.saleForm.get('variantId')?.setValue(null);
      this.variantAutocomplete?.resetInput();
      // Reload prices when customer changes
      this.prefillPrice(customer.id, this.saleForm.get('variantId')?.value);
      this.loadCurrentBalance();
      this.loadCurrentDue();
      this.cdr.markForCheck();
    } else {
      this.saleForm.get('customerId')?.setValue(null);
      this.variantAutocomplete?.resetInput();
      this.filteredVariants = this.variants;
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
      this.currentBalance = null;
      this.balanceLoading = false;
      this.balanceError = '';
      this.currentDue = null;
      this.dueLoading = false;
      this.dueError = '';
      this.cdr.markForCheck();
    }
  }

  onVariantSelected(variant: CylinderVariant | null): void {
    if (variant && variant.id) {
      // Set the entire variant object (not just ID) so valueChanges subscription receives the full object
      this.saleForm.get('variantId')?.setValue(variant, { emitEvent: true });
      this.saleForm.get('variantId')?.markAsTouched();
      // Reload prices when variant changes
      const customerObj = this.saleForm.get('customerId')?.value;
      const customerId = customerObj && customerObj.id ? customerObj.id : null;
      this.prefillPrice(customerId, variant.id);
      this.loadCurrentBalance();
    } else {
      this.saleForm.get('variantId')?.setValue(null);
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
      this.currentBalance = null;
      this.balanceLoading = false;
      this.balanceError = '';
    }
  }

  private loadCurrentBalance(): void {
    const customerObj = this.saleForm.get('customerId')?.value;
    const variantObj = this.saleForm.get('variantId')?.value;
    const customerId = customerObj && customerObj.id ? customerObj.id : null;
    const variantId = variantObj && variantObj.id ? variantObj.id : null;
    if (!customerId || !variantId) {
      this.currentBalance = null;
      this.balanceLoading = false;
      this.balanceError = '';
      return;
    }
    this.balanceLoading = true;
    this.balanceError = '';
    this.customerCylinderLedgerService.getCustomerVariantBalance(customerId, variantId)
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
    const customerObj = this.saleForm.get('customerId')?.value;
    const customerId = customerObj && customerObj.id ? customerObj.id : null;
    if (!customerId) {
      this.currentDue = null;
      this.dueLoading = false;
      this.dueError = '';
      return;
    }
    this.dueLoading = true;
    this.dueError = '';
    this.customerCylinderLedgerService.getCustomerDueAmounts([customerId])
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
   * Update payment mode validators based on amount received
   */
  private updatePaymentModeValidators(hasAmount: boolean): void {
    const modeControl = this.saleForm.get('modeOfPayment');
    const bankAccountControl = this.saleForm.get('bankAccountId');
    
    if (hasAmount) {
      modeControl?.setValidators(Validators.required);
    } else {
      modeControl?.clearValidators();
    }
    modeControl?.updateValueAndValidity({ emitEvent: false });
    this.cdr.markForCheck();
  }

  onSubmit() {
    if (!this.saleForm.valid) {
      this.saleForm.markAllAsTouched();
      this.toastr.error('Please correct the errors in the form.', 'Validation Error');
      return;
    }
    if (this.totalAmount <= 0) {
      this.toastr.error('Total amount must be greater than zero.', 'Validation Error');
      return;
    }
    const customerObj = this.saleForm.get('customerId')?.value;
    const variantObj = this.saleForm.get('variantId')?.value;
    const customerId = customerObj && customerObj.id ? customerObj.id : null;
    const variantId = variantObj && variantObj.id ? variantObj.id : null;
    const qtyIssued = parseInt(this.saleForm.get('filledIssuedQty')?.value);
    const qtyEmptyReceivedRaw = this.saleForm.get('emptyReceivedQty')?.value;
    const qtyEmptyReceived = isNaN(parseInt(qtyEmptyReceivedRaw)) ? 0 : parseInt(qtyEmptyReceivedRaw);
    // Prevent negative or non-integer empty returns
    if (qtyEmptyReceived < 0) {
      this.toastr.error('Empty cylinders returned must be zero or positive.', 'Validation Error');
      return;
    }
    if (qtyEmptyReceived > 0) {
      const sub = this.customerCylinderLedgerService.getCustomerVariantBalance(customerId, variantId)
        .pipe(
          catchError((err: unknown) => {
            const errorObj = err as { error?: { message?: string }; message?: string };
            const errorMessage = errorObj?.error?.message || errorObj?.message || 'Could not validate customer balance. Please try again.';
            this.toastr.error(errorMessage, 'Error');
            return of(null as number | null);
          })
        )
        .subscribe((balance: number | null) => {
          const allowedEmpty = (balance ?? 0) + (isNaN(qtyIssued) ? 0 : qtyIssued);
          if (balance !== null && qtyEmptyReceived > allowedEmpty) {
            this.toastr.error('Cannot return more empty cylinders than the customer will hold after this sale.', 'Validation Error');
            sub.unsubscribe();
            return;
          } else {
            this.submitSale();
            sub.unsubscribe();
          }
        });
    } else {
      this.submitSale();
    }
  }

  submitSale() {
    this.toastr.clear(); // Clear any previous notifications
    // Validate warehouse is selected
    const warehouseObj = this.saleForm.get('warehouseId')?.value;
    if (!warehouseObj || !warehouseObj.id) {
      this.toastr.error('Please select a warehouse', 'Validation Error');
      this.saleForm.get('warehouseId')?.markAsTouched();
      return;
    }

    // Validate form is valid
    if (this.saleForm.invalid) {
      this.toastr.error('Please fill all required fields', 'Validation Error');
      return;
    }

    const customerObj = this.saleForm.get('customerId')?.value;
    const variantObj = this.saleForm.get('variantId')?.value;
    const warehouseIdFromForm = this.saleForm.get('warehouseId')?.value;
    const customerId = customerObj && customerObj.id ? customerObj.id : null;
    const variantId = variantObj && variantObj.id ? variantObj.id : null;
    const warehouseId = warehouseIdFromForm && warehouseIdFromForm.id ? warehouseIdFromForm.id : null;
    const qtyIssued = parseInt(this.saleForm.get('filledIssuedQty')?.value);
    const qtyEmptyReceivedRaw = this.saleForm.get('emptyReceivedQty')?.value;
    const qtyEmptyReceived = isNaN(parseInt(qtyEmptyReceivedRaw)) ? 0 : parseInt(qtyEmptyReceivedRaw);
    const modeOfPayment = this.saleForm.get('modeOfPayment')?.value;
    const bankAccountIdValue = this.saleForm.get('bankAccountId')?.value;
    
    // Calculate total discount (per-unit discount × quantity)
    const totalDiscount = (this.discountPrice || 0) * qtyIssued;
    
    const saleDate = this.isStaff
      ? this.dateUtility.getTodayInIST()
      : (this.saleForm.get('saleDate')?.value || this.dateUtility.getTodayInIST());
    const saleRequest: CreateSaleRequest = {
      warehouseId: warehouseId,
      customerId: customerId,
      saleDate: saleDate,
      amountReceived: this.saleForm.get('amountReceived')?.value || 0,
      modeOfPayment: modeOfPayment,
      items: [
        {
          variantId: variantId,
          qtyIssued: qtyIssued,
          qtyEmptyReceived: qtyEmptyReceived,
          discount: totalDiscount
        } as SaleItemRequest
      ]
    };

    // Add bankAccountId if payment requires bank account
    const selectedPaymentMode = this.getSelectedPaymentMode(modeOfPayment);
    if (modeOfPayment && selectedPaymentMode?.isBankAccountRequired && bankAccountIdValue) {
      saleRequest.bankAccountId = bankAccountIdValue;
    }
    const sub = this.saleService.createSale(saleRequest)
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error recording sale. Please try again.';
          this.toastr.error(errorMessage, 'Error');
          return of(null as Sale | null);
        })
      )
      .subscribe({
        next: (response: Sale | null) => {
          if (response) {
            this.toastr.success('Sale recorded successfully', 'Success');
            // Notify dashboard of the sale
            this.dataRefreshService.notifySaleCreated(response);
            this.resetForm();
          }
        },
        complete: () => {
          sub.unsubscribe();
        }
      });
  }

  resetForm() {
    // Reset the autocomplete components
    if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
      this.autocompleteInputs.forEach(input => {
        input.resetInput();
      });
    }
    
    // Rebuild the form with correct validators (amountReceived and modeOfPayment are optional)
    this.saleForm = this.fb.group({
      warehouseId: [null, Validators.required],
      customerId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledIssuedQty: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      emptyReceivedQty: [null, [Validators.min(0), Validators.max(100)]],
      basePrice: [null, [Validators.required, Validators.min(0), Validators.max(10000)]],
      amountReceived: [null, [Validators.min(0), Validators.max(100000)]],
      modeOfPayment: [null],
      bankAccountId: [null],
      saleDate: [this.dateUtility.getTodayInIST(), Validators.required]
    });
    
    this.saleForm.get('basePrice')?.disable();
    
    // Re-add conditional validators for payment mode and bank account
    this.saleForm.get('amountReceived')?.valueChanges.subscribe(() => {
      const modeControl = this.saleForm.get('modeOfPayment');
      const bankAccountControl = this.saleForm.get('bankAccountId');
      const amountReceived = this.saleForm.get('amountReceived')?.value;
      
      if (amountReceived && amountReceived > 0) {
        modeControl?.setValidators(Validators.required);
      } else {
        modeControl?.clearValidators();
      }
      modeControl?.updateValueAndValidity();
      
      // Update form validity
      this.saleForm.updateValueAndValidity();
    });

    this.saleForm.get('modeOfPayment')?.valueChanges.subscribe((mode) => {
      const bankAccountControl = this.saleForm.get('bankAccountId');
      const selectedMode = this.getSelectedPaymentMode(mode);
      if (mode && selectedMode?.isBankAccountRequired) {
        bankAccountControl?.setValidators(Validators.required);
      } else {
        bankAccountControl?.clearValidators();
        bankAccountControl?.reset();
      }
      bankAccountControl?.updateValueAndValidity();
      
      // Update form validity
      this.saleForm.updateValueAndValidity();
    });
    
    this.saleForm.markAsPristine();
    this.saleForm.markAsUntouched();
    this.baseAmount = 0;
    this.discountPrice = 0;
    this.filteredVariants = this.variants;
    this.currentBalance = null;
    this.balanceLoading = false;
    this.balanceError = '';
    this.currentDue = null;
    this.dueLoading = false;
    this.dueError = '';
    this.applyStaffDateRestriction();
  }
}
