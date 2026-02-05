
import { catchError, of, finalize, debounceTime, distinctUntilChanged, map, Subject, takeUntil, BehaviorSubject } from 'rxjs';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators, FormArray, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faEdit, faTrash, faBook, faPlus, faTimes, faExclamation, faUsers, faEye, faEllipsisV, faDownload } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
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
  filteredCustomers$ = this.searchTerm$.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    map(term => this.filterCustomersImpl(term)),
    takeUntil(this.destroy$)
  );

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
  initialStockEntriesMap: { [variantId: number]: any } = {};



  isLastRow(customer: any): boolean {
    const index = this.paginatedCustomers.indexOf(customer);
    return index === this.paginatedCustomers.length - 1;
  }
  get isAnyDropdownOpen(): boolean {
    return this.paginatedCustomers && Array.isArray(this.paginatedCustomers)
      ? this.paginatedCustomers.some((c: any) => c && c.showMenu)
      : false;
  }


  getTotalPages() {
    return this.totalPages;
  }

  /**
   * OPTIMIZATION: Implement debounced search using RxJS
   * Before: Filtering happens on every keystroke (1000ms per keystroke)
   * After: Debounced to 300ms, prevents excessive change detection
   */
  get paginatedCustomers() {
    // Return current search result with debouncing already applied
    return this.filterCustomersImpl(this.searchTerm);
  }

  /**
   * Implementation of search filtering logic
   */
  private filterCustomersImpl(term: string) {
    if (!term || term.trim() === '') {
      return this.customers;
    }
    const searchTerm = term.trim().toLowerCase();
    return this.customers.filter(c =>
      (c.name && c.name.toLowerCase().includes(searchTerm)) ||
      (c.mobile && c.mobile.toLowerCase().includes(searchTerm)) ||
      (c.address && c.address.toLowerCase().includes(searchTerm))
    );
  }

  /**
   * Update search term with debouncing via BehaviorSubject
   */
  updateSearchTerm(term: string): void {
    this.searchTerm = term;
    this.searchTerm$.next(term);
    this.cdr.markForCheck();
  }
  customerForm!: FormGroup;
  showForm = false;
  editingId: number | null = null;
  showLedger = false;
  selectedCustomer: any = null;
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

  customers: any[] = [];
  ledgerEntries: any[] = [];
  variants: any[] = [];
  existingCustomerPrices: { [key: number]: any } = {};
  variantSummary: any[] = [];

  showDetailsModal = false;
  detailsCustomer: any = null;
  detailsVariantPrices: any[] = [];

  showReasonModal = false;
  selectedReasonEntry: any = null;

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
  selectedLedgerEntry: any = null;
  updateForm: any = {
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
    private dataRefreshService: DataRefreshService
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadVariantsAndCustomers();
    this.loadBankAccounts();
    this.loadPaymentModes();

    // Subscribe to debounced search changes
    this.filteredCustomers$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBankAccounts() {
    this.bankAccountService.getActiveBankAccounts()
      .subscribe({
        next: (response: any) => {
          this.bankAccounts = response || [];
          this.cdr.markForCheck();
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
  toggleCustomerMenu(customer: any) {
    // Close all other menus
    this.paginatedCustomers.forEach((c: any) => {
      if (c !== customer) {
        c.showMenu = false;
      }
    });
    // Toggle current menu
    customer.showMenu = !customer.showMenu;
  }

  openDetailsModal(customer: any) {
    this.detailsCustomer = customer;
    this.detailsVariantPrices = [];
    this.showDetailsModal = true;
    
    // Load variant prices for this customer
    this.variantPriceService.getPricesByCustomer(customer.id)
      .subscribe({
        next: (response: any) => {
          // Response is ApiResponse<List<CustomerVariantPriceDTO>>
          if (response && response.data) {
            this.detailsVariantPrices = response.data;
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error loading variant prices:', err);
          this.detailsVariantPrices = [];
        }
      });
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.detailsCustomer = null;
    this.detailsVariantPrices = [];
  }

  openReasonModal(entry: any) {
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
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading variants';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [] });
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: any) => {
        this.variants = data.content || data;
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

  loadCustomersWithBalances() {
    this.loadingService.show('Loading customers...');
    // Fetch both customers and their balances for the current page
    const customerSub = this.customerService.getAllCustomers(this.customerPage - 1, this.customerPageSize)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading customers';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: any) => {
        const customers = (data.content || data).sort((a: any, b: any) => b.id - a.id);
        this.totalCustomers = data.totalElements || customers.length;
        this.totalPages = data.totalPages || 1;
        // Now fetch balances for these customers in one batch
        this.ledgerService.getCustomerBalances(this.customerPage - 1, this.customerPageSize).subscribe({
          next: (balances: CustomerBalance[]) => {
            // Merge balances into customers array
            this.customers = customers.map((customer: any) => {
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
            const customerIds = this.customers.map(c => c.id);
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
          error: (err: any) => {
            this.customers = customers.map((customer: any) => ({ ...customer, returnPendingUnits: 0, filledUnits: 0 }));
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
    entries = entries.sort((a: any, b: any) => {
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
    if (this.canShowMoreLedger && this.selectedCustomer) {
      this.ledgerPage++;
      this.loadLedgerForCustomer(this.selectedCustomer.id, false); // false = don't reset page
    }
  }

  get totalPendingUnits(): number {
    // Group entries by variantName and pick the entry with the highest id (most recent)
    if (!this.filteredLedgerEntries.length) return 0;
    const mostRecentByVariant: { [variant: string]: { id: number, balance: number } } = {};
    for (const entry of this.filteredLedgerEntries) {
      if (
        !mostRecentByVariant[entry.variantName] ||
        entry.id > mostRecentByVariant[entry.variantName].id
      ) {
        mostRecentByVariant[entry.variantName] = { id: entry.id, balance: entry.balance };
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
      gstNo: ['', [Validators.pattern('^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[0-9A-Z]{2}$')]],      dueAmount: [0, [Validators.required, Validators.min(0)]],      active: [true, Validators.required],
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
  buildVariantPricingArray(selectedVariantIds?: any[]) {
    const pricesArray = this.customerForm.get('variantPrices') as FormArray;
    const variantIds = selectedVariantIds || this.customerForm.get('configuredVariants')?.value || [];
    
    // Clear and rebuild with selected variants
    pricesArray.clear();
    
    this.variants
      .filter(variant => variantIds.includes(variant.id))
      .forEach(variant => {
        // Get existing prices from backend load (this.existingCustomerPrices)
        const existingPrice = this.existingCustomerPrices[variant.id];
        // Use basePrice as default, or existing basePrice from backend
        const basePriceValue = existingPrice?.basePrice !== undefined ? existingPrice.basePrice : (variant.basePrice || '');
        // Use existing discountPrice from backend, or empty string
        const discountPriceValue = existingPrice?.discountPrice !== undefined ? existingPrice.discountPrice : '';
        
        const priceGroup = this.fb.group({
          variantId: [variant.id, Validators.required],
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
  buildVariantFilledCylindersArray(selectedVariantIds?: any[]) {
    const filledCylArray = this.customerForm.get('variantFilledCylinders') as FormArray;
    const variantIds = selectedVariantIds || this.customerForm.get('configuredVariants')?.value || [];
    
    // Create a map of existing filled cylinders by variantId for quick lookup
    const existingFilled: { [key: number]: any } = {};
    filledCylArray.controls.forEach((control: any) => {
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
      .filter(variant => variantIds.includes(variant.id))
      .forEach(variant => {
        // Get existing filled cylinders if available, otherwise empty
        const existingFil = existingFilled[variant.id];
        
        const filledCylGroup = this.fb.group({
          variantId: [variant.id, Validators.required],
          variantName: [variant.name, Validators.required],
          filledCylinders: [existingFil?.filledCylinders || '', [Validators.required, Validators.min(0)]]
        });
        filledCylArray.push(filledCylGroup);
        
        // Disable the field if it has an initial stock entry (pre-filled data)
        const initialStockEntry = this.initialStockEntriesMap[variant.id];
        if (initialStockEntry) {
          filledCylGroup.get('filledCylinders')?.setValue(initialStockEntry.filledOut);
          filledCylGroup.get('filledCylinders')?.disable();
        }
      });
  }

  /**
   * Check if a variant is selected in the configuration
   */
  isVariantSelected(variantId: number): boolean {
    const configuredVariants = this.customerForm.get('configuredVariants')?.value || [];
    return configuredVariants.includes(variantId);
  }

  /**
   * Toggle variant selection and rebuild pricing array
   */
  toggleVariantSelection(variantId: number) {
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

  editCustomer(customer: any) {
    this.showForm = true;
    this.editingId = customer.id;
    
    // Fetch fresh customer data from backend to ensure we have latest configured variants
    this.customerService.getCustomer(customer.id).subscribe({
      next: (freshCustomer: any) => {
        this.customerForm.patchValue({
          name: freshCustomer.name || '',
          mobile: freshCustomer.mobile || '',
          address: freshCustomer.address || '',
          gstNo: freshCustomer.gstNo || '',
          dueAmount: freshCustomer.dueAmount !== undefined ? freshCustomer.dueAmount : 0,
          active: freshCustomer.active !== undefined ? freshCustomer.active : true,
          configuredVariants: freshCustomer.configuredVariants || []
        });

        // Load existing variant prices from backend and initial filled cylinders
        if (freshCustomer.id) {
          // Load variant prices
          this.variantPriceService.getPricesByCustomer(freshCustomer.id).subscribe({
            next: (response: any) => {
              if (response && response.data && response.data.length > 0) {
            // Convert API response to form array format
            const prices = response.data;
            
            // Create a map of existing prices by variantId
            const existingPrices: { [key: number]: any } = {};
            prices.forEach((price: any) => {
              existingPrices[price.variantId] = {
                basePrice: price.salePrice,
                discountPrice: price.discountPrice
              };
            });
            
            // Store for buildVariantPricingArray to use
            this.existingCustomerPrices = existingPrices;
            
            // Rebuild the array with existing prices
            this.buildVariantPricingArray(freshCustomer.configuredVariants || []);
          } else {
            // No existing prices, rebuild with empty
            this.buildVariantPricingArray(freshCustomer.configuredVariants || []);
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error loading variant prices:', err);
          // Still rebuild the array even if load fails
          this.buildVariantPricingArray(freshCustomer.configuredVariants || []);
        }
      });

      // Load initial filled cylinders from customer ledger (INITIAL_STOCK entries)
      this.ledgerService.getLedgerByCustomer(freshCustomer.id).subscribe({
        next: (ledgerEntries: any[]) => {
          // Get INITIAL_STOCK entries to populate filled cylinders
          const initialStockEntries = ledgerEntries.filter(entry => entry.refType === 'INITIAL_STOCK');
          
          // Store in map for later use when rebuilding array
          this.initialStockEntriesMap = {};
          initialStockEntries.forEach(entry => {
            this.initialStockEntriesMap[entry.variantId] = entry;
          });
          
          // Rebuild to populate with initial values
          this.buildVariantFilledCylindersArray(freshCustomer.configuredVariants || []);
          
          // Re-apply the initial stock filled values and disable the fields
          const filledCylArray = this.customerForm.get('variantFilledCylinders') as FormArray;
          
          filledCylArray.controls.forEach((control: any) => {
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
          console.error('Error loading ledger entries:', err);
          // Still rebuild the array even if ledger load fails
          this.buildVariantFilledCylindersArray(freshCustomer.configuredVariants || []);
        }
      });
    }
      },
      error: (err) => {
        console.error('Error loading customer for edit:', err);
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
      dueAmount: formValue.dueAmount || 0,
      active: formValue.active,
      configuredVariants: formValue.configuredVariants,
      variantFilledCylinders: variantFilledCylinders
    };

    if (this.editingId) {
      // Update customer
      const sub = this.customerService.updateCustomer(this.editingId, customerData)
        .pipe(
          catchError((error: any) => {
            const errorMessage = error?.error?.message || error?.message || 'Error updating customer';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
            return of(null);
          })
        )
        .subscribe((updatedCustomer: any) => {
          if (updatedCustomer) {
            // Update variant prices (existing customer)
            this.updateVariantPrices(updatedCustomer.id, variantPrices, false);

            const index = this.customers.findIndex(c => c.id === this.editingId);
            if (index !== -1) {
              this.customers[index] = updatedCustomer;
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
          catchError((error: any) => {
            const errorMessage = error?.error?.message || error?.message || 'Error creating customer';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
            return of(null);
          })
        )
        .subscribe((newCustomer: any) => {
          if (newCustomer) {
            // Create variant prices for the new customer
            this.updateVariantPrices(newCustomer.id, variantPrices, true);

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
  private updateVariantPrices(customerId: number, variantPrices: any[], isNewCustomer: boolean = false) {
    // Get form array to access disabled controls
    const pricesArray = this.customerForm.get('variantPrices') as FormArray;
    
    pricesArray.controls.forEach((control: any, index: number) => {
      const basePrice = control.get('basePrice')?.value ? parseFloat(control.get('basePrice')?.value) : null;
      const discountPrice = control.get('discountPrice')?.value ? parseFloat(control.get('discountPrice')?.value) : null;
      const variantId = control.get('variantId')?.value;
      const variantName = control.get('variantName')?.value;
      
      // Use basePrice as salePrice and discountPrice is required
      if (basePrice !== null && discountPrice !== null) {
        const payload: any = {
          customerId: customerId,
          variantId: variantId,
          variantName: variantName,
          salePrice: basePrice,
          discountPrice: discountPrice
        };

        console.log('Saving variant pricing:', payload);

        if (isNewCustomer) {
          // For new customers, directly create
          this.variantPriceService.createPrice(customerId, payload)
            .subscribe({
              next: (response: any) => {
                console.log('Variant pricing created successfully for variant', variantId);
              },
              error: (error: any) => {
                console.error('Error creating variant pricing:', error);
              }
            });
        } else {
          // For existing customers, try UPDATE first (more likely case), then CREATE
          this.variantPriceService.updatePrice(customerId, variantId, payload)
            .subscribe({
              next: (response: any) => {
                console.log('Variant pricing updated successfully for variant', variantId);
              },
              error: (error: any) => {
                console.log('Update failed with status:', error?.status);
                
                // If update fails (404 means not found), try to create
                if (error?.status === 404) {
                  console.log('Pricing not found, attempting to create for variant', variantId);
                  this.variantPriceService.createPrice(customerId, payload)
                    .subscribe({
                      next: (response: any) => {
                        console.log('Variant pricing created successfully for variant', variantId);
                      },
                      error: (createError: any) => {
                        console.error('Error creating variant pricing:', createError);
                      }
                    });
                } else {
                  console.error('Error updating variant pricing:', error);
                }
              }
            });
        }
      }
    });
  }

  deleteCustomer(customer: any) {
    this.toastr.warning('This feature is not yet implemented.', 'Warning');
    return;
    const confirmDelete = confirm(`Are you sure you want to delete customer "${customer.name}"?`);
    if (confirmDelete) {
      const sub = this.customerService.deleteCustomer(customer.id)
        .pipe(
          catchError((error: any) => {
            const errorMessage = error?.error?.message || error?.message || 'Error deleting customer';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
            return of(null);
          })
        )
        .subscribe((result) => {
          if (result) {
            this.customers = this.customers.filter(c => c.id !== customer.id);
            this.customerService.invalidateCache();
            this.toastr.success('Customer deleted successfully.', 'Success');
          }
        });
    }
  }

  toggleLedger(customer: any) {
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
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading ledger';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 0 });
        }),
        finalize(() => {
          this.isLoadingMoreLedger = false;
          if (resetPage) this.loadingService.hide();
        })
      )
      .subscribe((data: any) => {
        // Sort by transaction date (ascending - oldest first) for accurate running balance calculation
        const newEntries = (data.content || data).sort((a: any, b: any) => {
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
        
        this.totalLedgerItems = data.totalElements || 0;
        this.totalLedgerPages = data.totalPages || 1;
        this.cdr.markForCheck();
        ledgerSub.unsubscribe();
        
        // Fetch and load the summary data for accurate per-variant summary
        if (resetPage) {
          this.ledgerService.getCustomerLedgerSummary(customerId).subscribe({
            next: (summaryData: any) => {
              // Update variant summary with data from backend
              if (summaryData && summaryData.variants) {
                this.variantSummary = summaryData.variants;
                this.cdr.markForCheck();
              }
            },
            error: (err) => {
              console.error('Error loading ledger summary:', err);
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
    if (!this.selectedCustomer) {
      this.toastr.warning('No customer selected', 'Warning');
      return;
    }

    // First, get the total count to know how many records exist
    this.loadingService.show('Preparing ledger export...');
    this.ledgerService.getLedgerByCustomerPaginated(this.selectedCustomer.id, 0, 1)
      .pipe(
        catchError((error: any) => {
          this.toastr.error('Failed to load ledger data', 'Error');
          this.loadingService.hide();
          return of({ content: [], totalElements: 0 });
        })
      )
      .subscribe((initialData: any) => {
        // Now fetch ALL records using the total count
        const totalRecords = initialData.totalElements || 0;
        
        if (totalRecords === 0) {
          this.toastr.warning('No ledger data to export', 'Warning');
          this.loadingService.hide();
          return;
        }

        // Fetch all records in one request
        this.ledgerService.getLedgerByCustomerPaginated(this.selectedCustomer.id, 0, totalRecords)
          .pipe(
            catchError((error: any) => {
              this.toastr.error('Failed to load ledger data', 'Error');
              this.loadingService.hide();
              return of({ content: [], totalElements: 0 });
            }),
            finalize(() => {
              this.loadingService.hide();
            })
          )
          .subscribe((data: any) => {
            const allLedgerEntries = (data.content || []).sort((a: any, b: any) => {
              const dateA = new Date(a.transactionDate).getTime();
              const dateB = new Date(b.transactionDate).getTime();
              return dateA - dateB; // Oldest first
            });

            if (allLedgerEntries.length === 0) {
              this.toastr.warning('No ledger data to export', 'Warning');
              return;
            }

            try {
              const totalDebit = allLedgerEntries.reduce((sum: number, entry: any) => {
                return sum + (entry.totalAmount || 0);
              }, 0);

              const totalCredit = allLedgerEntries.reduce((sum: number, entry: any) => {
                return sum + (entry.amountReceived || 0);
              }, 0);

              const balanceDue = totalDebit - totalCredit;

              exportCustomerLedgerToPDF({
                customerName: this.selectedCustomer.name || 'Customer',
                customerPhone: this.selectedCustomer.phone || this.selectedCustomer.mobile,
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
              console.error('Error exporting ledger to PDF:', error);
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
  getFilteredVariantSummary(): any[] {
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

  openPaymentFormForCustomer(customer: any) {
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

    // Get the current due amount from the most recent ledger entry
    const currentDue = this.ledgerEntries.length > 0 
      ? this.ledgerEntries[this.ledgerEntries.length - 1].dueAmount || 0 
      : 0;

    // Validate payment amount doesn't exceed due amount
    if (amountValue > currentDue) {
      this.toastr.error(`Payment amount cannot exceed due amount of ₹${currentDue.toFixed(2)}. Current payment: ₹${amountValue.toFixed(2)}`, 'Validation Error');
      return;
    }

    this.isSubmittingPayment = true;
    const paymentData: any = {
      customerId: this.selectedCustomer.id,
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
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording payment';
          this.toastr.error(errorMessage, 'Error');
          this.isSubmittingPayment = false;
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
          this.toastr.success('Payment recorded successfully', 'Success');
          this.closePaymentForm();
          // Refresh ledger
          this.loadLedgerForCustomer(this.selectedCustomer.id);
          // Update customer's due amount from ledger
          this.ledgerService.getLedgerByCustomerAll(this.selectedCustomer.id).subscribe({
            next: (allEntries: any[]) => {
              if (allEntries.length > 0) {
                // Sort chronologically (oldest first)
                const chronoEntries = allEntries.sort((a: any, b: any) => {
                  const dateA = new Date(a.transactionDate).getTime();
                  const dateB = new Date(b.transactionDate).getTime();
                  if (dateA === dateB) {
                    return (a.id || 0) - (b.id || 0);
                  }
                  return dateA - dateB;
                });
                
                // Calculate cumulative balance
                let cumulativeBalance = 0;
                chronoEntries.forEach((entry: any) => {
                  if (entry.refType === 'PAYMENT') {
                    cumulativeBalance -= (entry.amountReceived || 0);
                  } else {
                    const transactionDue = (entry.totalAmount || 0) - (entry.amountReceived || 0);
                    cumulativeBalance += transactionDue;
                  }
                });
                
                // Update customer's due amount in the table
                const customer = this.customers.find(c => c.id === this.selectedCustomer.id);
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

  canEditLedgerEntry(entry: any, allEntries: any[]): boolean {
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

  // ==================== UPDATE LEDGER ENTRY METHODS ====================

  openUpdateForm(ledgerEntry: any) {
    this.selectedLedgerEntry = ledgerEntry;
    this.updateForm = {
      filledOut: ledgerEntry.filledOut || 0,
      emptyIn: ledgerEntry.emptyIn || 0,
      totalAmount: ledgerEntry.totalAmount || 0,
      amountReceived: ledgerEntry.amountReceived || 0,
      updateReason: '',
      paymentMode: ledgerEntry.paymentMode || null,
      bankAccountId: ledgerEntry.bankAccountId || null
    };
    
    // Store original values for calculations
    this.originalFilledOut = ledgerEntry.filledOut || 0;
    this.originalAmountReceived = ledgerEntry.amountReceived || 0;
    
    // For SALE entries, calculate price per unit for auto-calculation
    if (ledgerEntry.refType === 'SALE' && ledgerEntry.filledOut > 0) {
      this.pricePerUnit = ledgerEntry.totalAmount / ledgerEntry.filledOut;
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
      const newTotalAmount = this.updateForm.filledOut * this.pricePerUnit;
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
  parseCurrencyToNumber(value: any): number {
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
    if (this.updateForm.filledOut < 0 || this.updateForm.emptyIn < 0) {
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
    const updateData: any = {};
    if (this.updateForm.filledOut !== this.selectedLedgerEntry.filledOut) {
      updateData.filledOut = this.updateForm.filledOut;
    }
    if (this.updateForm.emptyIn !== this.selectedLedgerEntry.emptyIn) {
      updateData.emptyIn = this.updateForm.emptyIn;
    }
    if (totalAmountNum !== this.selectedLedgerEntry.totalAmount) {
      updateData.totalAmount = totalAmountNum;
    }
    if (amountReceivedNum !== this.selectedLedgerEntry.amountReceived) {
      updateData.amountReceived = amountReceivedNum;
    }
    if (this.updateForm.paymentMode !== this.selectedLedgerEntry.paymentMode) {
      updateData.paymentMode = this.updateForm.paymentMode;
    }
    if (this.updateForm.bankAccountId !== this.selectedLedgerEntry.bankAccountId) {
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

    console.log('Updating ledger entry:', this.selectedLedgerEntry.id, updateData);

    this.ledgerService.updateLedgerEntry(this.selectedLedgerEntry.id, updateData)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error updating ledger entry';
          this.updateError = errorMessage;
          this.toastr.error(errorMessage, 'Update Failed');
          console.error('Update error:', error);
          this.isSubmittingUpdate = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
          this.toastr.success('Ledger entry updated successfully. All subsequent entries recalculated.', 'Success');
          this.closeUpdateForm();
          // Refresh ledger to show updated values
          this.loadLedgerForCustomer(this.selectedCustomer.id);
          // Update customer's due amount
          this.ledgerService.getLedgerByCustomerAll(this.selectedCustomer.id).subscribe({
            next: (allEntries: any[]) => {
              if (allEntries.length > 0) {
                // Get latest due from the most recent entry
                const latestEntry = allEntries[allEntries.length - 1];
                const customer = this.customers.find(c => c.id === this.selectedCustomer.id);
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
        this.isSubmittingUpdate = false;
        this.cdr.markForCheck();
      });
  }
}