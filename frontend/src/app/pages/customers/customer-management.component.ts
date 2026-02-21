
import { catchError, of, finalize, debounceTime, distinctUntilChanged, Subject, takeUntil, BehaviorSubject, skip } from 'rxjs';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators, FormArray, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faEdit, faTrash, faBook, faPlus, faTimes, faExclamation, faUsers, faEye, faEllipsisV, faDownload } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { CustomerBalance, VariantBalance } from '../../models/customer-balance.model';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { CustomerVariantPriceService } from '../../services/customer-variant-price.service';
import { LoadingService } from '../../services/loading.service';
import { ToastrService } from 'ngx-toastr';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { exportCustomerLedgerToPDF } from './export-ledger-report.util';
import { BankAccount } from '../../models/bank-account.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { DateUtilityService } from '../../services/date-utility.service';
import { SharedModule } from '../../shared/shared.module';
import { DataRefreshService } from '../../services/data-refresh.service';
import { SimpleStatusDTO } from '../../models/simple-status';
import { Customer } from '../../models/customer.model';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { CustomerLedgerVariantSummary } from '../../models/customer-ledger-summary.model';
import { CustomerVariantPrice } from '../../models/customer-variant-price.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { LedgerUpdateRequest } from '../../models/ledger-update-request.model';
import { PageResponse } from '../../models/page-response';
import { PaymentsSummary } from '../../models/payments-summary.model';

type CustomerRow = Customer & {
  id: number;
  showMenu?: boolean;
  returnPendingUnits?: number;
  filledUnits?: number;
  dueAmount?: number;
};

@Component({
  selector: 'app-customer-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule, AutocompleteInputComponent, SharedModule],
  templateUrl: './customer-management.component.html',
  styleUrl: './customer-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerManagementComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private searchTerm$ = new BehaviorSubject<string>('');
  /**
   * Custom validator: ensures discountPrice <= salePrice
   */
  discountNotGreaterThanSaleValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.parent) return null;
    
    const salePrice = control.parent.get('salePrice')?.value;
    const discountPrice = control.value;
    
    // If both values exist and discount is greater than sale, return error
    if (salePrice !== null && salePrice !== undefined && salePrice !== '' &&
        discountPrice !== null && discountPrice !== undefined && discountPrice !== '') {
      if (parseFloat(discountPrice) > parseFloat(salePrice)) {
        return { discountGreaterThanSale: true };
      }
    }
    return null;
  }

  // Pagination state for customers table
  customerPage = 1;
  customerPageSize = 10;
  totalCustomers = 0;
  totalPages = 1;

  // Ledger paging state
  ledgerPage = 1;
  ledgerPageSize = 5;
  totalLedgerItems = 0;
  totalLedgerPages = 1;
  isLoadingMoreLedger = false;

  // Store initial stock entries to maintain disabled state when rebuilding
  initialStockEntriesMap: Record<number, CustomerCylinderLedger> = {};



  isLastRow(customer: CustomerRow): boolean {
    const index = this.paginatedCustomers.indexOf(customer);
    return index === this.paginatedCustomers.length - 1;
  }
  get isAnyDropdownOpen(): boolean {
    return this.paginatedCustomers && Array.isArray(this.paginatedCustomers)
      ? this.paginatedCustomers.some((c: CustomerRow) => c && c.showMenu)
      : false;
  }


  getTotalPages() {
    return this.totalPages;
  }

  get paginatedCustomers() {
    return this.customers;
  }

  updateSearchTerm(term: string): void {
    this.searchTerm = term;
    this.searchTerm$.next(term);
    this.cdr.markForCheck();
  }
  customerForm!: FormGroup;
  showForm = false;
  editingId: number | null = null;
  showLedger = false;
  selectedCustomer: CustomerRow | null = null;
  searchTerm = '';

  ledgerFilterVariant: string = '';

  // Font Awesome Icons
  faSearch = faSearch;
  faEdit = faEdit;
  faTrash = faTrash;
  faBook = faBook;
  faPlus = faPlus;
  faTimes = faTimes;
  faExclamation = faExclamation;
  faEye = faEye;
  faUsers = faUsers;
  faEllipsisV = faEllipsisV;
  faDownload = faDownload;

  customers: CustomerRow[] = [];
  ledgerEntries: CustomerCylinderLedger[] = [];
  variants: CylinderVariant[] = [];
  existingCustomerPrices: Record<number, { basePrice?: number; discountPrice?: number }> = {};
  variantSummary: CustomerLedgerVariantSummary[] = [];

  showDetailsModal = false;
  detailsCustomer: CustomerRow | null = null;
  detailsVariantPrices: CustomerVariantPrice[] = [];
  users: User[] = [];

  showReasonModal = false;
  selectedReasonEntry: CustomerCylinderLedger | null = null;

  // Rename all pendingUnits to returnPendingUnits for clarity
  // Add property to store filled units
  // This will be set per customer
  showPaymentForm = false;
  isSubmittingPayment = false;
  paymentError = '';
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  paymentForm: { amount: string | number | null; paymentDate: string; paymentMode: string; bankAccountId?: number | null } = {
    amount: null,
    paymentDate: '',
    paymentMode: '',
    bankAccountId: null
  };
  //filledUnits: number = 0;

  // UPDATE LEDGER FORM
  showUpdateForm = false;
  isSubmittingUpdate = false;
  updateError = '';
  selectedLedgerEntry: CustomerCylinderLedger | null = null;
  updateForm: LedgerUpdateRequest = {
    filledOut: 0,
    emptyIn: 0,
    totalAmount: 0,
    amountReceived: 0
  };
  pricePerUnit = 0; // For SALE entries, original price per unit
  originalFilledOut = 0; // Track original qty to calc price per unit
  originalAmountReceived = 0; // Track original payment ratio

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private ledgerService: CustomerCylinderLedgerService,
    private bankAccountService: BankAccountService,
    private paymentModeService: PaymentModeService,
    private variantService: CylinderVariantService,
    private variantPriceService: CustomerVariantPriceService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    public cdr: ChangeDetectorRef,
    private dateUtility: DateUtilityService,
    private dataRefreshService: DataRefreshService,
    private userService: UserService
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadVariantsAndCustomers();
    this.loadBankAccounts();
    this.loadPaymentModes();
    this.loadUsers();

    // Subscribe to debounced search changes
    this.searchTerm$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        skip(1),
        takeUntil(this.destroy$)
      )
      .subscribe((term) => {
        this.customerPage = 1;
        this.loadCustomersWithBalances(term);
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBankAccounts() {
    this.bankAccountService.getActiveBankAccounts()
      .subscribe({
        next: (response: BankAccount[]) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
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
        }
      });
  }

  getSelectedPaymentMode(modeName: string): PaymentMode | undefined {
    return this.paymentModes.find(mode => mode.name === modeName);
  }

  /**
   * Converts paymentForm.amount to numeric value, handling formatted strings
   */
  getPaymentAmountAsNumber(): number {
    if (!this.paymentForm.amount) return 0;
    if (typeof this.paymentForm.amount === 'string') {
      return parseFloat(this.paymentForm.amount.replace(/,/g, '')) || 0;
    }
    return this.paymentForm.amount || 0;
  }

  onPaymentAmountInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.paymentForm.amount = input.value;
    this.cdr.markForCheck();
  }
  get defaultVariantFilter() {
    return { id: null, name: 'All' };
  }

  get variantsWithAll() {
    return [
      { id: null, name: 'All' },
      { id: null, name: 'Payment' },
      ...this.variants
    ];
  }

  get filteredVariants() {
    if (!this.ledgerFilterVariant) return this.variants;
    return this.variants.filter(v => v.name === this.ledgerFilterVariant);
  }

  /**
   * Toggle action menu for a specific customer
   * Closes all other dropdowns and toggles the current one
   */
  toggleCustomerMenu(customer: CustomerRow) {
    // Close all other menus
    this.paginatedCustomers.forEach((c: CustomerRow) => {
      if (c !== customer) {
        c.showMenu = false;
      }
    });
    // Toggle current menu
    customer.showMenu = !customer.showMenu;
  }

  openDetailsModal(customer: CustomerRow) {
    this.detailsCustomer = customer;
    this.detailsVariantPrices = [];
    this.showDetailsModal = true;
    
    // Load variant prices for this customer
    this.variantPriceService.getPricesByCustomer(customer.id)
      .subscribe({
        next: (response: CustomerVariantPrice[]) => {
          if (response) {
            this.detailsVariantPrices = response;
          }
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.detailsVariantPrices = [];
        }
      });
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.detailsCustomer = null;
    this.detailsVariantPrices = [];
  }

  getCreatedByName(createdBy?: string | null): string {
    if (!createdBy) {
      return 'N/A';
    }
    const user = this.users.find(u => u.username === createdBy);
    return user?.name || createdBy;
  }

  private loadUsers() {
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

  openReasonModal(entry: CustomerCylinderLedger) {
    this.selectedReasonEntry = entry;
    this.showReasonModal = true;
  }

  closeReasonModal() {
    this.showReasonModal = false;
    this.selectedReasonEntry = null;
  }

  loadVariantsAndCustomers() {
    this.loadingService.show('Loading variants...');
    const variantSub = this.variantService.getAllVariantsWithCache()
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading variants';
          this.toastr.error(errorMessage, 'Error');
          return of([]);
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: CylinderVariant[]) => {
        this.variants = data || [];
        this.buildVariantPricingArray(); // Build form array with variants
        this.loadCustomersWithBalances();
        variantSub.unsubscribe();
      });
  }

  onPageChange(page: number) {
    this.customerPage = page;
    this.loadCustomersWithBalances();
  }

  onPageSizeChange(size: number) {
    this.customerPageSize = size;
    this.customerPage = 1;
    this.loadCustomersWithBalances();
  }

  loadCustomersWithBalances(searchTerm: string = this.searchTerm) {
    this.loadingService.show('Loading customers...');
    // Fetch both customers and their balances for the current page
    const customerSub = this.customerService.getAllCustomers(
      this.customerPage - 1,
      this.customerPageSize,
      'id',
      'ASC',
      searchTerm
    )
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading customers';
          this.toastr.error(errorMessage, 'Error');
          return of({
            items: [],
            totalElements: 0,
            totalPages: 1,
            page: this.customerPage - 1,
            size: this.customerPageSize
          } as PageResponse<CustomerRow>);
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: PageResponse<Customer>) => {
        const customers = (data.items || [])
          .filter((customer): customer is CustomerRow => typeof customer.id === 'number')
          .sort((a, b) => b.id - a.id);
        this.totalCustomers = data.totalElements ?? customers.length;
        this.totalPages = data.totalPages ?? 1;
        // Now fetch balances for these customers in one batch
        const customerIds = customers.map(c => c.id);
        this.ledgerService.getCustomerBalancesByIds(customerIds).subscribe({
          next: (balances: CustomerBalance[]) => {
            // Merge balances into customers array
            this.customers = customers.map((customer: CustomerRow) => {
              const balanceObj = balances.find(b => b.customerId === customer.id);
              let returnPendingUnits = 0;
              let filledUnits = 0;
              if (balanceObj && balanceObj.variantBalances) {
                for (const vb of balanceObj.variantBalances) {
                  if (vb.balance > 0) {
                    returnPendingUnits += vb.balance;
                    filledUnits += vb.balance;
                  }
                }
              }
              return { ...customer, returnPendingUnits, filledUnits };
            });
            
            // Fetch due amounts in a single call to avoid N+1
            this.ledgerService.getCustomerDueAmounts(customerIds).subscribe({
              next: (dueMap: { [key: number]: number }) => {
                this.customers.forEach(customer => {
                  const due = dueMap[customer.id];
                  customer.dueAmount = due !== undefined ? due : 0;
                });
                this.cdr.markForCheck();
              },
              error: () => {
                // Keep original dueAmount on error
                this.cdr.markForCheck();
              }
            });
            
            this.cdr.markForCheck();
          },
          error: (err: unknown) => {
            this.customers = customers.map((customer: CustomerRow) => ({ ...customer, returnPendingUnits: 0, filledUnits: 0 }));
            this.cdr.markForCheck();
          }
        });
        customerSub.unsubscribe();
      });
  }



  // Remove filteredCustomers for backend pagination

  get filteredLedgerEntries() {
    let entries = this.ledgerEntries;
    if (this.ledgerFilterVariant) {
      if (this.ledgerFilterVariant === 'Payment') {
        // Filter to show only PAYMENT transactions
        entries = entries.filter(e => e.refType === 'PAYMENT');
      } else {
        // Filter by variant, but always show PAYMENT transactions (they apply to all variants)
        entries = entries.filter(e => e.variantName === this.ledgerFilterVariant || e.refType === 'PAYMENT');
      }
    }
    // Sort by transaction date (newest first) for display
    entries = entries.sort((a: CustomerCylinderLedger, b: CustomerCylinderLedger) => {
      const dateA = new Date(a.transactionDate).getTime();
      const dateB = new Date(b.transactionDate).getTime();
      if (dateA === dateB) {
        // If same date, sort by ID descending (newer entries have higher IDs)
        return (b.id || 0) - (a.id || 0);
      }
      return dateB - dateA; // Newest first
    });
    return entries;
  }

  get canShowMoreLedger() {
    return (this.ledgerPage * this.ledgerPageSize) < this.totalLedgerItems;
  }

  showMoreLedger() {
    const customerId = this.selectedCustomer?.id;
    if (this.canShowMoreLedger && customerId) {
      this.ledgerPage++;
      this.loadLedgerForCustomer(customerId, false); // false = don't reset page
    }
  }

  get totalPendingUnits(): number {
    // Group entries by variantName and pick the entry with the highest id (most recent)
    if (!this.filteredLedgerEntries.length) return 0;
    const mostRecentByVariant: { [variant: string]: { id: number, balance: number } } = {};
    for (const entry of this.filteredLedgerEntries) {
      const entryId = entry.id ?? 0;
      if (
        !mostRecentByVariant[entry.variantName] ||
        entryId > mostRecentByVariant[entry.variantName].id
      ) {
        mostRecentByVariant[entry.variantName] = { id: entryId, balance: entry.balance };
      }
    }
    return Object.values(mostRecentByVariant).reduce((sum, v) => sum + (typeof v.balance === 'number' ? v.balance : 0), 0);
  }

  getVariantColor(variantName: string): string {
    if (!variantName) return '#888';
    const colors = ['#1976d2', '#388e3c', '#fbc02d', '#d32f2f', '#7b1fa2', '#0288d1', '#c2185b'];
    let hash = 0;
    for (let i = 0; i < variantName.length; i++) {
      hash = variantName.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  initForm() {
    this.customerForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      mobile: ['', [Validators.required, Validators.pattern('^[6-9][0-9]{9}$')]],
      address: ['', [Validators.required, Validators.maxLength(255)]],
      gstNo: ['', [Validators.pattern('^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[0-9A-Z]{2}$')]],
      securityDeposit: [0, [Validators.min(0)]],
      dueAmount: [0, [Validators.required, Validators.min(0)]],
      active: [true, Validators.required],
      configuredVariants: [[], Validators.required],
      variantFilledCylinders: this.fb.array([]),
      variantPrices: this.fb.array([])
    });

    // Listen for variant configuration changes
    this.customerForm.get('configuredVariants')?.valueChanges.subscribe(selectedVariantIds => {
      this.buildVariantFilledCylindersArray(selectedVariantIds);
      this.buildVariantPricingArray(selectedVariantIds);
    });
  }

  /**
   * Build variant pricing form controls only for selected variants
   * Preserves existing prices when variants are added/removed
   */
  buildVariantPricingArray(selectedVariantIds?: number[]) {
    const pricesArray = this.customerForm.get('variantPrices') as FormArray;
    const variantIds = selectedVariantIds || this.customerForm.get('configuredVariants')?.value || [];
    
    // Clear and rebuild with selected variants
    pricesArray.clear();
    
    this.variants
      .filter((variant): variant is CylinderVariant & { id: number } =>
        typeof variant.id === 'number' && variantIds.includes(variant.id))
      .forEach(variant => {
        const variantId = variant.id;
        // Get existing prices from backend load (this.existingCustomerPrices)
        const existingPrice = this.existingCustomerPrices[variantId];
        // Use basePrice as default, or existing basePrice from backend
        const basePriceValue = existingPrice?.basePrice !== undefined ? existingPrice.basePrice : (variant.basePrice || '');
        // Use existing discountPrice from backend, or empty string
        const discountPriceValue = existingPrice?.discountPrice !== undefined ? existingPrice.discountPrice : '';
        
        const priceGroup = this.fb.group({
          variantId: [variantId, Validators.required],
          variantName: [variant.name],
          basePrice: [basePriceValue, { value: basePriceValue, disabled: true }], // Read-only basePrice
          discountPrice: [discountPriceValue, [Validators.required, Validators.min(0)]]
        });
        pricesArray.push(priceGroup);
      });
  }

  get variantPricesArray(): FormArray {
    return this.customerForm.get('variantPrices') as FormArray;
  }

  get variantFilledCylindersArray(): FormArray {
    return this.customerForm.get('variantFilledCylinders') as FormArray;
  }

  /**
   * Build variant filled cylinders form controls for each selected variant
   */
  buildVariantFilledCylindersArray(selectedVariantIds?: number[]) {
    const filledCylArray = this.customerForm.get('variantFilledCylinders') as FormArray;
    const variantIds = selectedVariantIds || this.customerForm.get('configuredVariants')?.value || [];
    
    // Create a map of existing filled cylinders by variantId for quick lookup
    const existingFilled: Record<number, { filledCylinders?: number; variantName?: string }> = {};
    filledCylArray.controls.forEach((control) => {
      const variantId = control.get('variantId')?.value;
      if (variantId) {
        existingFilled[variantId] = {
          filledCylinders: control.get('filledCylinders')?.value,
          variantName: control.get('variantName')?.value
        };
      }
    });

    // Clear and rebuild with selected variants
    filledCylArray.clear();
    
    this.variants
      .filter((variant): variant is CylinderVariant & { id: number } =>
        typeof variant.id === 'number' && variantIds.includes(variant.id))
      .forEach(variant => {
        const variantId = variant.id;
        // Get existing filled cylinders if available, otherwise empty
        const existingFil = existingFilled[variantId];
        
        const filledCylGroup = this.fb.group({
          variantId: [variantId, Validators.required],
          variantName: [variant.name, Validators.required],
          filledCylinders: [existingFil?.filledCylinders || '', [Validators.required, Validators.min(0)]]
        });
        filledCylArray.push(filledCylGroup);
        
        // Disable the field if it has an initial stock entry (pre-filled data)
        const initialStockEntry = this.initialStockEntriesMap[variantId];
        if (initialStockEntry) {
          filledCylGroup.get('filledCylinders')?.setValue(initialStockEntry.filledOut);
          filledCylGroup.get('filledCylinders')?.disable();
        }
      });
  }

  /**
   * Check if a variant is selected in the configuration
   */
  isVariantSelected(variantId: number | undefined): boolean {
    if (variantId === undefined || variantId === null) {
      return false;
    }
    const configuredVariants = this.customerForm.get('configuredVariants')?.value || [];
    return configuredVariants.includes(variantId);
  }

  /**
   * Toggle variant selection and rebuild pricing array
   */
  toggleVariantSelection(variantId: number | undefined) {
    if (variantId === undefined || variantId === null) {
      return;
    }
    const configuredVariantsControl = this.customerForm.get('configuredVariants');
    const currentValues = configuredVariantsControl?.value || [];
    
    if (currentValues.includes(variantId)) {
      // Remove variant
      configuredVariantsControl?.setValue(currentValues.filter((id: number) => id !== variantId));
    } else {
      // Add variant
      configuredVariantsControl?.setValue([...currentValues, variantId]);
    }
  }

  // (removed duplicate openAddForm)

  closeForm() {
    this.showForm = false;
    this.customerForm.reset();
  }

  openAddForm() {
    this.showForm = true;
    this.editingId = null;
    this.customerForm.reset();
    this.customerForm.patchValue({ active: true, configuredVariants: [] });
    // Clear the initial stock entries map for new customer
    this.initialStockEntriesMap = {};
    // Clear existing prices for new customer
    this.existingCustomerPrices = {};
  }

  editCustomer(customer: CustomerRow) {
    this.showForm = true;
    this.editingId = customer.id;
    
    // Fetch fresh customer data from backend to ensure we have latest configured variants
    this.customerService.getCustomer(customer.id).subscribe({
      next: (freshCustomer: Customer) => {
        if (typeof freshCustomer.id !== 'number') {
          this.toastr.error('Customer data is missing an id', 'Error');
          return;
        }
        const customerRow = freshCustomer as CustomerRow;
        this.customerForm.patchValue({
          name: customerRow.name || '',
          mobile: customerRow.mobile || '',
          address: customerRow.address || '',
          gstNo: customerRow.gstNo || '',
          securityDeposit: customerRow.securityDeposit !== undefined ? customerRow.securityDeposit : 0,
          dueAmount: customerRow.dueAmount !== undefined ? customerRow.dueAmount : 0,
          active: customerRow.active !== undefined ? customerRow.active : true,
          configuredVariants: customerRow.configuredVariants || []
        });

        // Load existing variant prices from backend and initial filled cylinders
        if (customerRow.id) {
          // Load variant prices
          this.variantPriceService.getPricesByCustomer(customerRow.id).subscribe({
            next: (response: CustomerVariantPrice[]) => {
              if (response && response.length > 0) {
            // Convert API response to form array format
            const prices = response;
            
            // Create a map of existing prices by variantId
            const existingPrices: Record<number, { basePrice?: number; discountPrice?: number }> = {};
            prices.forEach((price: CustomerVariantPrice) => {
              existingPrices[price.variantId] = {
                basePrice: price.salePrice,
                discountPrice: price.discountPrice
              };
            });
            
            // Store for buildVariantPricingArray to use
            this.existingCustomerPrices = existingPrices;
            
            // Rebuild the array with existing prices
            this.buildVariantPricingArray(customerRow.configuredVariants || []);
          } else {
            // No existing prices, rebuild with empty
            this.buildVariantPricingArray(customerRow.configuredVariants || []);
          }
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          // Still rebuild the array even if load fails
          this.buildVariantPricingArray(customerRow.configuredVariants || []);
        }
      });

      // Load initial filled cylinders from customer ledger (INITIAL_STOCK entries)
      this.ledgerService.getLedgerByCustomer(customerRow.id).subscribe({
        next: (ledgerEntries: CustomerCylinderLedger[]) => {
          // Get INITIAL_STOCK entries to populate filled cylinders
          const initialStockEntries = ledgerEntries.filter(entry => entry.refType === 'INITIAL_STOCK');
          
          // Store in map for later use when rebuilding array
          this.initialStockEntriesMap = {};
          initialStockEntries.forEach(entry => {
            this.initialStockEntriesMap[entry.variantId] = entry;
          });
          
          // Rebuild to populate with initial values
          this.buildVariantFilledCylindersArray(customerRow.configuredVariants || []);
          
          // Re-apply the initial stock filled values and disable the fields
          const filledCylArray = this.customerForm.get('variantFilledCylinders') as FormArray;
          
          filledCylArray.controls.forEach((control) => {
            const variantId = control.get('variantId')?.value;
            const initialStockEntry = this.initialStockEntriesMap[variantId];
            
            if (initialStockEntry) {
              control.get('filledCylinders')?.setValue(initialStockEntry.filledOut);
              // Disable the field since it's pre-filled from ledger
              control.get('filledCylinders')?.disable();
            }
          });
          
          this.cdr.markForCheck();
        },
        error: (err) => {
          // Still rebuild the array even if ledger load fails
          this.buildVariantFilledCylindersArray(freshCustomer.configuredVariants || []);
        }
      });
    }
      },
      error: (err) => {
        this.toastr.error('Failed to load customer details', 'Error');
      }
    });
  }

  saveCustomer() {
    if (!this.customerForm.valid) {
      this.customerForm.markAllAsTouched();
      this.toastr.error('Please correct the errors in the form.', 'Validation Error');
      return;
    }
    // Prevent duplicate mobile on frontend
    const mobile = this.customerForm.get('mobile')?.value;
    const duplicate = this.customers.some(c => c.mobile === mobile && c.id !== this.editingId);
    if (duplicate) {
      this.toastr.error('A customer with this mobile number already exists.', 'Validation Error');
      return;
    }

    // Separate customer data from variant pricing data
    const formValue = this.customerForm.value;
    const variantPrices = formValue.variantPrices || [];
    const variantFilledCylinders = formValue.variantFilledCylinders || [];
    const customerData = {
      name: formValue.name,
      mobile: formValue.mobile,
      address: formValue.address,
      gstNo: formValue.gstNo || null,
      securityDeposit: formValue.securityDeposit || 0,
      dueAmount: formValue.dueAmount || 0,
      active: formValue.active,
      configuredVariants: formValue.configuredVariants,
      variantFilledCylinders: variantFilledCylinders
    };

    if (this.editingId) {
      // Update customer
      const sub = this.customerService.updateCustomer(this.editingId, customerData)
        .pipe(
          catchError((error: unknown) => {
            const err = error as { error?: { message?: string }; message?: string };
            const errorMessage = err?.error?.message || err?.message || 'Error updating customer';
            this.toastr.error(errorMessage, 'Error');
            return of(null as CustomerRow | null);
          })
        )
        .subscribe((updatedCustomer: Customer | null) => {
          if (updatedCustomer && typeof updatedCustomer.id === 'number') {
            const updatedRow = updatedCustomer as CustomerRow;
            // Update variant prices (existing customer)
            this.updateVariantPrices(updatedRow.id, variantPrices, false);

            const index = this.customers.findIndex(c => c.id === this.editingId);
            if (index !== -1) {
              this.customers[index] = updatedRow;
            }
            this.customerService.invalidateCache();
            
            // Notify other components (like sale-entry) that a customer was updated
            this.dataRefreshService.notifyCustomerUpdated();
            
            this.toastr.success('Customer updated successfully.', 'Success');
            this.showForm = false;
            this.customerForm.reset();
            this.cdr.markForCheck();
          }
        });
    } else {
      // Create customer
      const sub = this.customerService.createCustomer(customerData)
        .pipe(
          catchError((error: unknown) => {
            const err = error as { error?: { message?: string }; message?: string };
            const errorMessage = err?.error?.message || err?.message || 'Error creating customer';
            this.toastr.error(errorMessage, 'Error');
            return of(null as CustomerRow | null);
          })
        )
        .subscribe((newCustomer: Customer | null) => {
          if (newCustomer && typeof newCustomer.id === 'number') {
            const newRow = newCustomer as CustomerRow;
            // Create variant prices for the new customer
            this.updateVariantPrices(newRow.id, variantPrices, true);

            this.customerService.invalidateCache();
            this.toastr.success('Customer added successfully.', 'Success');
            this.showForm = false;
            this.customerForm.reset();
            // Reload all customers with balances after adding new customer
            this.loadCustomersWithBalances();
            this.cdr.markForCheck();
          }
        });
    }
  }

  /**
   * Save variant prices for a customer
   * For existing customers: try UPDATE first, then CREATE if not found
   * For new customers: CREATE directly
   */
  private updateVariantPrices(customerId: number, variantPrices: CustomerVariantPrice[], isNewCustomer: boolean = false) {
    // Get form array to access disabled controls
    const pricesArray = this.customerForm.get('variantPrices') as FormArray;
    
    pricesArray.controls.forEach((control, index: number) => {
      const basePrice = control.get('basePrice')?.value ? parseFloat(control.get('basePrice')?.value) : null;
      const discountPrice = control.get('discountPrice')?.value ? parseFloat(control.get('discountPrice')?.value) : null;
      const variantId = control.get('variantId')?.value;
      const variantName = control.get('variantName')?.value;
      
      // Use basePrice as salePrice and discountPrice is required
      if (basePrice !== null && discountPrice !== null) {
        const payload: CustomerVariantPrice = {
          customerId: customerId,
          variantId: variantId,
          variantName: variantName,
          salePrice: basePrice,
          discountPrice: discountPrice
        };

        if (isNewCustomer) {
          // For new customers, directly create
          this.variantPriceService.createPrice(customerId, payload)
            .subscribe({
              next: (_response: CustomerVariantPrice) => {
              },
              error: (error: unknown) => {
              }
            });
        } else {
          // For existing customers, try UPDATE first (more likely case), then CREATE
          this.variantPriceService.updatePrice(customerId, variantId, payload)
            .subscribe({
              next: (_response: CustomerVariantPrice) => {
              },
              error: (error: unknown) => {
                const err = error as { status?: number };
                
                // If update fails (404 means not found), try to create
                if (err?.status === 404) {
                  this.variantPriceService.createPrice(customerId, payload)
                    .subscribe({
                      next: (_response: CustomerVariantPrice) => {
                      },
                      error: (createError: unknown) => {
                      }
                    });
                } else {
                }
              }
            });
        }
      }
    });
  }

  deleteCustomer(customer: CustomerRow) {
    this.toastr.warning('This feature is not yet implemented.', 'Warning');
    return;
    const confirmDelete = confirm(`Are you sure you want to delete customer "${customer.name}"?`);
    if (confirmDelete) {
      const sub = this.customerService.deleteCustomer(customer.id)
        .pipe(
          catchError((error: unknown) => {
            const err = error as { error?: { message?: string }; message?: string };
            const errorMessage = err?.error?.message || err?.message || 'Error deleting customer';
            this.toastr.error(errorMessage, 'Error');
            return of(null as SimpleStatusDTO | null);
          })
        )
        .subscribe((result: SimpleStatusDTO | null) => {
          if (result) {
            this.customers = this.customers.filter(c => c.id !== customer.id);
            this.customerService.invalidateCache();
            this.toastr.success('Customer deleted successfully.', 'Success');
          }
        });
    }
  }

  toggleLedger(customer: CustomerRow) {
    if (this.selectedCustomer && this.selectedCustomer.id === customer.id) {
      this.showLedger = !this.showLedger;
    } else {
      this.showLedger = true;
    }
    this.selectedCustomer = customer;
    if (this.showLedger) {
      this.loadLedgerForCustomer(customer.id);
    } else {
      this.ledgerEntries = [];
    }
  }

  loadLedgerForCustomer(customerId: number, resetPage: boolean = true) {
    if (resetPage) {
      this.ledgerPage = 1;
      this.ledgerFilterVariant = ''; // Reset filter to show all by default
    }
    
    if (resetPage) {
      this.loadingService.show('Loading ledger...');
    }
    this.isLoadingMoreLedger = true;
    const ledgerSub = this.ledgerService.getLedgerByCustomerPaginated(customerId, this.ledgerPage - 1, this.ledgerPageSize)
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading ledger';
          this.toastr.error(errorMessage, 'Error');
          return of({
            items: [],
            totalElements: 0,
            totalPages: 0,
            page: this.ledgerPage - 1,
            size: this.ledgerPageSize
          } as PageResponse<CustomerCylinderLedger>);
        }),
        finalize(() => {
          this.isLoadingMoreLedger = false;
          if (resetPage) this.loadingService.hide();
        })
      )
      .subscribe((data: PageResponse<CustomerCylinderLedger>) => {
        // Sort by transaction date (ascending - oldest first) for accurate running balance calculation
        const newEntries = (data.items || []).sort((a, b) => {
          const dateA = new Date(a.transactionDate).getTime();
          const dateB = new Date(b.transactionDate).getTime();
          return dateA - dateB; // Oldest first
        });
        
        if (resetPage) {
          this.ledgerEntries = newEntries;
        } else {
          // Append new entries for "show more"
          this.ledgerEntries = [...this.ledgerEntries, ...newEntries];
        }
        
        this.totalLedgerItems = data.totalElements ?? 0;
        this.totalLedgerPages = data.totalPages ?? 1;
        this.cdr.markForCheck();
        ledgerSub.unsubscribe();
        
        // Fetch and load the summary data for accurate per-variant summary
        if (resetPage) {
          this.ledgerService.getCustomerLedgerSummary(customerId).subscribe({
            next: (summaryData: { variants?: CustomerLedgerVariantSummary[] }) => {
              // Update variant summary with data from backend
              if (summaryData && summaryData.variants) {
                this.variantSummary = summaryData.variants;
                this.cdr.markForCheck();
              }
            },
            error: (err: unknown) => {
            }
          });
        }
      });
  }

  applyVariantFilter() {
    // Re-calculate pending and filled units based on the filtered ledger entries
    this.customers.forEach(customer => {
      let pendingTotal = 0;
      let filledTotal = 0;
      customer.returnPendingUnits = 0;
      customer.filledUnits = 0;
      this.filteredLedgerEntries.forEach(entry => {
        if (entry.customerId === customer.id) {
          pendingTotal += Math.abs(entry.balance < 0 ? entry.balance : 0);
          filledTotal += Math.max(entry.balance, 0);
        }
      });
      customer.returnPendingUnits = pendingTotal;
      customer.filledUnits = filledTotal;
    });
  }

  // Add missing methods for ledger modal
  viewLedger(customerId: number) {
    const customer = this.customers.find(c => c.id === customerId);
    if (customer) {
      this.toggleLedger(customer);
    }
  }

  closeLedger() {
    this.showLedger = false;
    this.selectedCustomer = null;
    this.ledgerEntries = [];
  }

  exportLedgerToPDF() {
    const selectedCustomer = this.selectedCustomer;
    if (!selectedCustomer) {
      this.toastr.warning('No customer selected', 'Warning');
      return;
    }

    // First, get the total count to know how many records exist
    this.loadingService.show('Preparing ledger export...');
    this.ledgerService.getLedgerByCustomerPaginated(selectedCustomer.id, 0, 1)
      .pipe(
        catchError((error: unknown) => {
          this.toastr.error('Failed to load ledger data', 'Error');
          this.loadingService.hide();
          return of({
            items: [],
            totalElements: 0,
            totalPages: 0,
            page: 0,
            size: 1
          } as PageResponse<CustomerCylinderLedger>);
        })
      )
      .subscribe((initialData: PageResponse<CustomerCylinderLedger>) => {
        // Now fetch ALL records using the total count
        const totalRecords = initialData.totalElements ?? 0;
        
        if (totalRecords === 0) {
          this.toastr.info('No data to export');
          this.loadingService.hide();
          return;
        }

        // Fetch all records in one request
        this.ledgerService.getLedgerByCustomerPaginated(selectedCustomer.id, 0, totalRecords)
          .pipe(
            catchError((error: unknown) => {
              this.toastr.error('Failed to load ledger data', 'Error');
              this.loadingService.hide();
              return of({
                items: [],
                totalElements: 0,
                totalPages: 0,
                page: 0,
                size: totalRecords
              } as PageResponse<CustomerCylinderLedger>);
            }),
            finalize(() => {
              this.loadingService.hide();
            })
          )
          .subscribe((data: PageResponse<CustomerCylinderLedger>) => {
            const allLedgerEntries = (data.items || []).sort((a, b) => {
              const dateA = new Date(a.transactionDate).getTime();
              const dateB = new Date(b.transactionDate).getTime();
              return dateA - dateB; // Oldest first
            });

            if (allLedgerEntries.length === 0) {
              this.toastr.info('No data to export');
              return;
            }

            try {
              const totalDebit = allLedgerEntries.reduce((sum: number, entry) => {
                return sum + (entry.totalAmount || 0);
              }, 0);

              const totalCredit = allLedgerEntries.reduce((sum: number, entry) => {
                return sum + (entry.amountReceived || 0);
              }, 0);

              const balanceDue = totalDebit - totalCredit;

              exportCustomerLedgerToPDF({
                customerName: selectedCustomer.name || 'Customer',
                customerPhone: selectedCustomer.mobile,
                ledgerData: allLedgerEntries,
                totalDebit: totalDebit,
                totalCredit: totalCredit,
                balanceDue: balanceDue,
                businessName: 'GAS AGENCY SYSTEM',
                fromDate: undefined,
                toDate: undefined
              });

              this.toastr.success('Ledger exported to PDF successfully', 'Success');
            } catch (error) {
              this.toastr.error('Failed to export ledger to PDF', 'Error');
            }
          });
      });
  }

  // Calculate total amount from all ledger entries
  getTotalAmount(): number {
    return this.filteredLedgerEntries.reduce((sum, entry) => {
      return sum + (entry.totalAmount || 0);
    }, 0);
  }

  // Calculate total amount received from all ledger entries
  getTotalAmountReceived(): number {
    return this.filteredLedgerEntries.reduce((sum, entry) => {
      return sum + (entry.amountReceived || 0);
    }, 0);
  }

  // Calculate total due amount from all ledger entries
  getTotalDueAmount(): number {
    return this.filteredLedgerEntries.reduce((sum, entry) => {
      return sum + (entry.dueAmount || 0);
    }, 0);
  }

  // Get due amount for a specific row (Total Amount - Amount Received for that transaction)
  getRowDueAmount(rowIndex: number): number {
    const entry = this.filteredLedgerEntries[rowIndex];
    const dueAmount = (entry.totalAmount || 0) - (entry.amountReceived || 0);
    return Math.max(0, dueAmount);
  }

  // Get previous due received for a specific row (cumulative till that row)
  getRowPreviousDueReceived(rowIndex: number): number {
    if (rowIndex === 0) return 0;
    
    let previousDueReceived = 0;
    for (let i = 0; i < rowIndex; i++) {
      const entry = this.filteredLedgerEntries[i];
      const dueThatDay = (entry.totalAmount || 0) - (entry.amountReceived || 0);
      previousDueReceived += dueThatDay;
    }
    
    return previousDueReceived;
  }

  // Get current balance owing for a specific row (use dueAmount directly from backend)
  getRowCurrentBalance(rowIndex: number): number {
    // Get the entry from filtered (display) view
    const filteredEntry = this.filteredLedgerEntries[rowIndex];
    
    // Simply return the dueAmount from the backend - it's already calculated as the running balance
    return filteredEntry.dueAmount || 0;
  }

  // Calculate total filled out cylinders from all ledger entries
  getTotalFilledOut(): number {
    return this.filteredLedgerEntries.reduce((sum, entry) => {
      return sum + (entry.filledOut || 0);
    }, 0);
  }

  // Calculate total return pending from variant summary
  getTotalReturnPending(): number {
    return this.getFilteredVariantSummary().reduce((sum, variant) => {
      return sum + (variant.returnPending || 0);
    }, 0);
  }

  // Generate a summary of changes for audit trail
  generateChangesSummary(): string {
    if (!this.selectedLedgerEntry) {
      return '';
    }

    const changes: string[] = [];
    const refType = this.selectedLedgerEntry.refType;

    // Parse currency strings to numbers
    const totalAmountNum = this.parseCurrencyToNumber(this.updateForm.totalAmount);
    const amountReceivedNum = this.parseCurrencyToNumber(this.updateForm.amountReceived);

    // For SALE: show if filled, empty, or total/payment changed
    if (refType === 'SALE') {
      if (this.updateForm.filledOut !== this.selectedLedgerEntry.filledOut) {
        changes.push(`Filled: ${this.selectedLedgerEntry.filledOut} → ${this.updateForm.filledOut}`);
      }
      if (this.updateForm.emptyIn !== this.selectedLedgerEntry.emptyIn) {
        changes.push(`Empty: ${this.selectedLedgerEntry.emptyIn} → ${this.updateForm.emptyIn}`);
      }
      if (totalAmountNum !== this.selectedLedgerEntry.totalAmount) {
        changes.push(`Total: ₹${this.selectedLedgerEntry.totalAmount?.toFixed(2)} → ₹${totalAmountNum.toFixed(2)}`);
      }
      if (amountReceivedNum !== this.selectedLedgerEntry.amountReceived) {
        changes.push(`Received: ₹${this.selectedLedgerEntry.amountReceived?.toFixed(2)} → ₹${amountReceivedNum.toFixed(2)}`);
      }
    }

    // For EMPTY_RETURN: show only empty count and payment change
    if (refType === 'EMPTY_RETURN') {
      if (this.updateForm.emptyIn !== this.selectedLedgerEntry.emptyIn) {
        changes.push(`Empty Count: ${this.selectedLedgerEntry.emptyIn} → ${this.updateForm.emptyIn}`);
      }
      if (amountReceivedNum !== this.selectedLedgerEntry.amountReceived) {
        changes.push(`Payment: ₹${this.selectedLedgerEntry.amountReceived?.toFixed(2)} → ₹${amountReceivedNum.toFixed(2)}`);
      }
    }

    // For PAYMENT: show only payment change
    if (refType === 'PAYMENT') {
      if (amountReceivedNum !== this.selectedLedgerEntry.amountReceived) {
        changes.push(`Payment: ₹${this.selectedLedgerEntry.amountReceived?.toFixed(2)} → ₹${amountReceivedNum.toFixed(2)}`);
      }
    }

    if (changes.length === 0) {
      return '';
    }

    return 'Changes:\n' + changes.map(c => '• ' + c).join('\n');
  }

  // Get variant summary filtered by selected variant
  getFilteredVariantSummary(): CustomerLedgerVariantSummary[] {
    if (!this.ledgerFilterVariant) {
      return this.variantSummary;
    }
    return this.variantSummary.filter(v => v.variantName === this.ledgerFilterVariant);
  }

  // Calculate total empty in cylinders from all ledger entries
  getTotalEmptyIn(): number {
    return this.filteredLedgerEntries.reduce((sum, entry) => {
      return sum + (entry.emptyIn || 0);
    }, 0);
  }

  // Calculate total balance pending (cylinders with customer)
  getTotalBalance(): number {
    if (this.filteredLedgerEntries.length === 0) return 0;
    // Balance is cumulative, so we take the last entry's balance which represents the net balance
    const sortedEntries = [...this.filteredLedgerEntries].sort((a, b) => 
      new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
    );
    
    let totalBalance = 0;
    const variantBalances = new Map<string, number>();
    
    // Iterate from oldest to newest to get the final balance per variant
    for (let i = sortedEntries.length - 1; i >= 0; i--) {
      const entry = sortedEntries[i];
      variantBalances.set(entry.variantName, entry.balance);
    }
    
    // Sum all variant balances
    variantBalances.forEach(balance => {
      totalBalance += balance;
    });
    
    return totalBalance;
  }

  // Get net filled units for a variant in the ledger modal (industry standard)
  getFilledUnitsForVariant(variantName: string): number {
    const entries = this.ledgerEntries.filter(e => e.variantName === variantName);
    if (!entries.length) return 0;
    // Find the most recent entry for this variant (ledgerEntries should be sorted newest to oldest)
    const mostRecentEntry = entries[0];
    return mostRecentEntry.balance || 0;
  }

  // Remove confusing pending calculation (industry standard: only show net filled)
  getPendingUnitsForVariant(variantName: string): number {
    return 0;
  }

  // Payment form methods
  openPaymentForm() {
    // Validate customer is active
    if (this.selectedCustomer && !this.selectedCustomer.active) {
      this.toastr.error('Cannot record payment for inactive customer', 'Error');
      return;
    }
    this.paymentForm = {
      amount: null,
      paymentDate: this.dateUtility.getTodayInIST(),
      paymentMode: '',
      bankAccountId: null
    };
    this.paymentError = '';
    this.showPaymentForm = true;
  }

  openPaymentFormForCustomer(customer: CustomerRow) {
    this.selectedCustomer = customer;
    this.loadLedgerForCustomer(customer.id);
    this.openPaymentForm();
  }

  closePaymentForm() {
    this.showPaymentForm = false;
    this.paymentForm = { amount: null, paymentDate: '', paymentMode: '', bankAccountId: null };
    this.paymentError = '';
  }

  submitPayment() {
    if (!this.selectedCustomer) {
      this.toastr.error('No customer selected', 'Error');
      return;
    }
    if (!this.paymentForm.amount) {
      this.toastr.error('Please enter a valid payment amount', 'Validation Error');
      return;
    }

    if (!this.paymentForm.paymentDate) {
      this.toastr.error('Please select a payment date', 'Validation Error');
      return;
    }

    if (!this.paymentForm.paymentMode) {
      this.toastr.error('Please select a payment mode', 'Validation Error');
      return;
    }

    // If payment mode requires bank account, validate bank account is selected
    const selectedMode = this.getSelectedPaymentMode(this.paymentForm.paymentMode);
    const isBankAccountRequired = selectedMode ? selectedMode.isBankAccountRequired : false;
    
    if (isBankAccountRequired && !this.paymentForm.bankAccountId) {
      this.toastr.error('Please select a bank account for this payment mode', 'Validation Error');
      return;
    }

    // Convert amount string (formatted like "1,00,000.00") to number
    const amountValue = typeof this.paymentForm.amount === 'string' 
      ? parseFloat(this.paymentForm.amount.replace(/,/g, ''))
      : this.paymentForm.amount;

    // Validate amount is a valid number
    if (isNaN(amountValue) || amountValue <= 0) {
      this.toastr.error('Please enter a valid payment amount', 'Validation Error');
      return;
    }

    const currentDue = this.getCurrentDueAmount();

    // Validate payment amount doesn't exceed due amount
    if (amountValue > currentDue) {
      this.toastr.error(`Payment amount cannot exceed due amount of ₹${currentDue.toFixed(2)}. Current payment: ₹${amountValue.toFixed(2)}`, 'Validation Error');
      return;
    }

    this.isSubmittingPayment = true;
    const selectedCustomerId = this.selectedCustomer.id;
    const paymentData: { customerId: number; amount: number; paymentDate: string; paymentMode?: string; bankAccountId?: number } = {
      customerId: selectedCustomerId,
      amount: amountValue,
      paymentDate: this.paymentForm.paymentDate,
      paymentMode: this.paymentForm.paymentMode
    };

    // Add bankAccountId if required by payment mode configuration
    if (isBankAccountRequired && this.paymentForm.bankAccountId) {
      paymentData.bankAccountId = this.paymentForm.bankAccountId;
    }

    this.ledgerService.recordPayment(paymentData)
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error recording payment';
          this.toastr.error(errorMessage, 'Error');
          this.isSubmittingPayment = false;
          return of(null as CustomerCylinderLedger | null);
        })
      )
      .subscribe((response: CustomerCylinderLedger | null) => {
        if (response) {
          this.toastr.success('Payment recorded successfully', 'Success');
          this.closePaymentForm();
          // Refresh ledger
          this.loadLedgerForCustomer(selectedCustomerId);
          // Update customer's due amount from ledger
          this.ledgerService.getLedgerByCustomerAll(selectedCustomerId).subscribe({
            next: (allEntries: CustomerCylinderLedger[]) => {
              if (allEntries.length > 0) {
                // Sort chronologically (oldest first)
                const chronoEntries = allEntries.sort((a, b) => {
                  const dateA = new Date(a.transactionDate).getTime();
                  const dateB = new Date(b.transactionDate).getTime();
                  if (dateA === dateB) {
                    return (a.id || 0) - (b.id || 0);
                  }
                  return dateA - dateB;
                });
                
                // Calculate cumulative balance
                let cumulativeBalance = 0;
                chronoEntries.forEach((entry) => {
                  if (entry.refType === 'PAYMENT') {
                    cumulativeBalance -= (entry.amountReceived || 0);
                  } else {
                    const transactionDue = (entry.totalAmount || 0) - (entry.amountReceived || 0);
                    cumulativeBalance += transactionDue;
                  }
                });
                
                // Update customer's due amount in the table
                const selectedCustomerId = this.selectedCustomer?.id;
                const customer = selectedCustomerId
                  ? this.customers.find(c => c.id === selectedCustomerId)
                  : undefined;
                if (customer) {
                  customer.dueAmount = cumulativeBalance;
                  this.cdr.markForCheck();
                }
              }
            },
            error: () => {
              // Silently fail on error
            }
          });
        }
        this.isSubmittingPayment = false;
      });
  }

  canEditLedgerEntry(entry: CustomerCylinderLedger, allEntries: CustomerCylinderLedger[]): boolean {
    if (!allEntries || allEntries.length === 0) {
      return false;
    }

    // Allow editing only if entry is within the latest 15 records
    const maxEditableIndex = Math.max(0, allEntries.length - 15);
    const entryIndex = allEntries.findIndex(e => e.id === entry.id);
    return entryIndex >= maxEditableIndex;
  }

  getPaymentModeLabel(mode: string): string {
    const modeLabels: { [key: string]: string } = {
      'CASH': 'Cash',
      'CHEQUE': 'Cheque',
      'BANK_TRANSFER': 'Online',
      'CREDIT': 'Credit',
      'UPI': 'UPI'
    };
    return modeLabels[mode] || mode;
  }

  getCurrentDueAmount(): number {
    const ledgerDue = this.calculateDueFromLedgerEntries(this.ledgerEntries);
    if (ledgerDue !== null) {
      return ledgerDue;
    }
    const tableDue = this.selectedCustomer?.dueAmount;
    return typeof tableDue === 'number' ? tableDue : 0;
  }

  private calculateDueFromLedgerEntries(entries: CustomerCylinderLedger[]): number | null {
    if (!entries || entries.length === 0) {
      return null;
    }
    const sorted = [...entries].sort((a, b) => {
      const dateA = new Date(a.transactionDate).getTime();
      const dateB = new Date(b.transactionDate).getTime();
      if (dateA === dateB) {
        return (a.id || 0) - (b.id || 0);
      }
      return dateA - dateB;
    });
    let cumulative = 0;
    sorted.forEach((entry) => {
      if (entry.refType === 'PAYMENT' || entry.refType === 'CREDIT') {
        cumulative -= (entry.amountReceived || 0);
      } else {
        const transactionDue = (entry.totalAmount || 0) - (entry.amountReceived || 0);
        cumulative += transactionDue;
      }
    });
    return cumulative;
  }

  // ==================== UPDATE LEDGER ENTRY METHODS ====================

  openUpdateForm(ledgerEntry: CustomerCylinderLedger) {
    this.selectedLedgerEntry = ledgerEntry;
    this.updateForm = {
      filledOut: ledgerEntry.filledOut || 0,
      emptyIn: ledgerEntry.emptyIn || 0,
      totalAmount: ledgerEntry.totalAmount || 0,
      amountReceived: ledgerEntry.amountReceived || 0,
      updateReason: '',
      paymentMode: ledgerEntry.paymentMode || undefined,
      bankAccountId: ledgerEntry.bankAccountId || undefined
    };
    
    // Store original values for calculations
    this.originalFilledOut = ledgerEntry.filledOut || 0;
    this.originalAmountReceived = ledgerEntry.amountReceived || 0;
    
    // For SALE entries, calculate price per unit for auto-calculation
    if (ledgerEntry.refType === 'SALE' && ledgerEntry.filledOut > 0) {
      const totalAmount = ledgerEntry.totalAmount ?? 0;
      this.pricePerUnit = totalAmount / ledgerEntry.filledOut;
    } else {
      this.pricePerUnit = 0;
    }
    
    this.updateError = '';
    this.showUpdateForm = true;
    this.cdr.markForCheck();
  }

  // Auto-calculate totalAmount when filledOut changes for SALE entries
  onFilledOutChange() {
    if (this.selectedLedgerEntry?.refType === 'SALE' && this.pricePerUnit > 0) {
      // Recalculate totalAmount based on new filledOut and original price per unit
      const newTotalAmount = (this.updateForm.filledOut ?? 0) * this.pricePerUnit;
      this.updateForm.totalAmount = Math.round(newTotalAmount * 100) / 100; // Round to 2 decimals
      
      // amountReceived stays as-is - user can edit it manually
      this.cdr.markForCheck();
    }
  }

  closeUpdateForm() {
    this.showUpdateForm = false;
    this.selectedLedgerEntry = null;
    this.updateForm = {
      filledOut: 0,
      emptyIn: 0,
      totalAmount: 0,
      amountReceived: 0,
      updateReason: ''
    };
    this.updateError = '';
    this.cdr.markForCheck();
  }

  // Helper function to convert currency-formatted string to number
  parseCurrencyToNumber(value: string | number | null | undefined): number {
    if (typeof value === 'number') {
      return value;
    }
    if (typeof value === 'string') {
      // Remove commas and convert to number
      return parseFloat(value.replace(/,/g, ''));
    }
    return 0;
  }

  submitUpdate() {
    if (!this.selectedLedgerEntry) {
      this.toastr.error('No entry selected', 'Error');
      return;
    }

    // Validate update reason is provided
    if (!this.updateForm.updateReason || this.updateForm.updateReason.trim() === '') {
      this.toastr.error('Update reason is required', 'Validation Error');
      return;
    }

    // Convert currency strings to numbers
    const amountReceivedNum = this.parseCurrencyToNumber(this.updateForm.amountReceived);
    const totalAmountNum = this.parseCurrencyToNumber(this.updateForm.totalAmount);

    // If amount received is greater than 0, payment mode is required
    if (amountReceivedNum > 0 && !this.updateForm.paymentMode) {
      this.toastr.error('Payment mode is required when amount received is greater than 0', 'Validation Error');
      return;
    }

    // Validate bank account if payment mode requires it
    if (this.updateForm.paymentMode) {
      const selectedMode = this.getSelectedPaymentMode(this.updateForm.paymentMode);
      if (selectedMode && selectedMode.isBankAccountRequired && !this.updateForm.bankAccountId) {
        this.toastr.error('Bank account is required for this payment mode', 'Validation Error');
        return;
      }
    }

    // Validate values
    const filledOutVal = this.updateForm.filledOut ?? 0;
    const emptyInVal = this.updateForm.emptyIn ?? 0;
    if (filledOutVal < 0 || emptyInVal < 0) {
      this.updateError = 'Filled/Empty count cannot be negative';
      this.cdr.markForCheck();
      return;
    }

    if (totalAmountNum < 0 || amountReceivedNum < 0) {
      this.updateError = 'Total or received amounts cannot be negative';
      this.cdr.markForCheck();
      return;
    }

    // NOTE: amountReceived CAN exceed totalAmount - customer can overpay to settle cumulative due
    // The backend will handle this and cap the cumulative due to 0

    this.isSubmittingUpdate = true;
    this.updateError = '';

    // Only send fields that were actually changed
    const updateData: Partial<LedgerUpdateRequest> = {};
    if (filledOutVal !== (this.selectedLedgerEntry.filledOut ?? 0)) {
      updateData.filledOut = filledOutVal;
    }
    if (emptyInVal !== (this.selectedLedgerEntry.emptyIn ?? 0)) {
      updateData.emptyIn = emptyInVal;
    }
    if (totalAmountNum !== (this.selectedLedgerEntry.totalAmount ?? 0)) {
      updateData.totalAmount = totalAmountNum;
    }
    if (amountReceivedNum !== (this.selectedLedgerEntry.amountReceived ?? 0)) {
      updateData.amountReceived = amountReceivedNum;
    }
    if (this.updateForm.paymentMode !== (this.selectedLedgerEntry.paymentMode ?? null)) {
      updateData.paymentMode = this.updateForm.paymentMode;
    }
    if (this.updateForm.bankAccountId !== (this.selectedLedgerEntry.bankAccountId ?? null)) {
      updateData.bankAccountId = this.updateForm.bankAccountId;
    }
    
    // Generate changes summary for the reason
    const changesSummary = this.generateChangesSummary();
    
    // Include updateReason if provided (optional)
    if (this.updateForm.updateReason && this.updateForm.updateReason.trim()) {
      // Prepend changes summary to user's reason
      updateData.updateReason = changesSummary + '\n\nUser Note: ' + this.updateForm.updateReason.trim();
    } else if (changesSummary) {
      // If no user reason, just save the changes summary
      updateData.updateReason = changesSummary;
    }

    // If no changes, show warning
    if (Object.keys(updateData).filter(key => key !== 'updateReason').length === 0) {
      this.updateError = 'No changes detected';
      this.isSubmittingUpdate = false;
      this.cdr.markForCheck();
      return;
    }

    const ledgerEntryId = this.selectedLedgerEntry.id;
    if (!ledgerEntryId) {
      this.updateError = 'Invalid ledger entry selected';
      this.isSubmittingUpdate = false;
      this.cdr.markForCheck();
      return;
    }
    this.ledgerService.updateLedgerEntry(ledgerEntryId, updateData)
      .pipe(
        catchError((error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error updating ledger entry';
          this.updateError = errorMessage;
          this.toastr.error(errorMessage, 'Update Failed');
          this.isSubmittingUpdate = false;
          this.cdr.markForCheck();
          return of(null as CustomerCylinderLedger | null);
        })
      )
      .subscribe((response: CustomerCylinderLedger | null) => {
        if (response) {
          this.toastr.success('Ledger entry updated successfully. All subsequent entries recalculated.', 'Success');
          this.closeUpdateForm();
          // Refresh ledger to show updated values
          const selectedCustomerId = this.selectedCustomer?.id;
          if (selectedCustomerId) {
            this.loadLedgerForCustomer(selectedCustomerId);
          }
          // Refresh customer list balances in table
          this.loadCustomersWithBalances();
          // Update customer's due amount
          if (selectedCustomerId) {
            this.ledgerService.getLedgerByCustomerAll(selectedCustomerId).subscribe({
            next: (allEntries: CustomerCylinderLedger[]) => {
              if (allEntries.length > 0) {
                // Get latest due from the most recent entry
                const latestEntry = allEntries[allEntries.length - 1];
                const customer = this.customers.find(c => c.id === selectedCustomerId);
                if (customer && latestEntry) {
                  customer.dueAmount = latestEntry.dueAmount || 0;
                  this.cdr.markForCheck();
                }
              }
            },
            error: () => {
              // Silently fail on error
            }
          });
          }
        }
        this.isSubmittingUpdate = false;
        this.cdr.markForCheck();
      });
  }
}