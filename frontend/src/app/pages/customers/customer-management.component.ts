
import { catchError, of, finalize } from 'rxjs';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators, FormArray, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faPencil, faTrash, faBook, faPlus, faTimes, faExclamation, faUsers, faEye, faEllipsisV, faDownload } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { CustomerBalance, VariantBalance } from '../../models/customer-balance.model';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { CustomerVariantPriceService } from '../../services/customer-variant-price.service';
import { LoadingService } from '../../services/loading.service';
import { ToastrService } from 'ngx-toastr';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { exportCustomerLedgerToPDF } from './export-ledger-report.util';
import { BankAccount } from '../../models/bank-account.model';

@Component({
  selector: 'app-customer-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule, AutocompleteInputComponent],
  templateUrl: './customer-management.component.html',
  styleUrl: './customer-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerManagementComponent implements OnInit, OnDestroy {

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
  get paginatedCustomers() {
    if (!this.searchTerm || this.searchTerm.trim() === '') {
      return this.customers;
    }
    const term = this.searchTerm.trim().toLowerCase();
    return this.customers.filter(c =>
      (c.name && c.name.toLowerCase().includes(term)) ||
      (c.mobile && c.mobile.toLowerCase().includes(term)) ||
      (c.address && c.address.toLowerCase().includes(term))
    );
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
  faPencil = faPencil;
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
  variantSummary: any[] = [];

  showDetailsModal = false;
  detailsCustomer: any = null;
  detailsVariantPrices: any[] = [];


  // Rename all pendingUnits to returnPendingUnits for clarity
  // Add property to store filled units
  // This will be set per customer
  showPaymentForm = false;
  isSubmittingPayment = false;
  paymentError = '';
  bankAccounts: BankAccount[] = [];
  paymentForm: { amount: number | null; paymentDate: string; paymentMode: string; bankAccountId?: number | null } = {
    amount: null,
    paymentDate: '',
    paymentMode: '',
    bankAccountId: null
  };
  //filledUnits: number = 0;

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private ledgerService: CustomerCylinderLedgerService,
    private bankAccountService: BankAccountService,
    private variantService: CylinderVariantService,
    private variantPriceService: CustomerVariantPriceService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadVariantsAndCustomers();
    this.loadBankAccounts();
  }

  ngOnDestroy() {}

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

  loadVariantsAndCustomers() {
    this.loadingService.show('Loading variants...');
    const variantSub = this.variantService.getAllVariants(0, 100)
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
            
            // Now fetch all ledger entries for each customer to calculate current due
            this.customers.forEach(customer => {
              this.ledgerService.getLedgerByCustomerPaginated(customer.id, 0, 1000).subscribe({
                next: (data: any) => {
                  const allEntries = data.content || data;
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
                    
                    // Update customer's due amount
                    customer.dueAmount = cumulativeBalance;
                  }
                  this.cdr.markForCheck();
                },
                error: () => {
                  // Keep original dueAmount on error
                  this.cdr.markForCheck();
                }
              });
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
    
    // Create a map of existing prices by variantId for quick lookup
    const existingPrices: { [key: number]: any } = {};
    pricesArray.controls.forEach((control: any, index: number) => {
      const variantId = control.get('variantId')?.value;
      if (variantId) {
        existingPrices[variantId] = {
          salePrice: control.get('salePrice')?.value,
          discountPrice: control.get('discountPrice')?.value,
          variantName: control.get('variantName')?.value
        };
      }
    });

    // Clear and rebuild with selected variants
    pricesArray.clear();
    
    this.variants
      .filter(variant => variantIds.includes(variant.id))
      .forEach(variant => {
        // Get existing prices if available, otherwise empty
        const existingPrice = existingPrices[variant.id];
        
        const priceGroup = this.fb.group({
          variantId: [variant.id, Validators.required],
          variantName: [variant.name],
          salePrice: [existingPrice?.salePrice || '', [Validators.required, Validators.min(0)]],
          discountPrice: [existingPrice?.discountPrice || '', [Validators.required, Validators.min(0), this.discountNotGreaterThanSaleValidator.bind(this)]]
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
  }

  editCustomer(customer: any) {
    this.showForm = true;
    this.editingId = customer.id;
    this.customerForm.patchValue({
      name: customer.name || '',
      mobile: customer.mobile || '',
      address: customer.address || '',
      gstNo: customer.gstNo || '',
      dueAmount: customer.dueAmount !== undefined ? customer.dueAmount : 0,
      active: customer.active !== undefined ? customer.active : true,
      configuredVariants: customer.configuredVariants || []
    });

    // Load existing variant prices from backend and initial filled cylinders
    if (customer.id) {
      // Load variant prices
      this.variantPriceService.getPricesByCustomer(customer.id).subscribe({
        next: (response: any) => {
          if (response && response.data && response.data.length > 0) {
            // Convert API response to form array format
            const pricesArray = this.customerForm.get('variantPrices') as FormArray;
            const prices = response.data;
            
            // Map prices back to form controls
            pricesArray.clear();
            prices.forEach((price: any) => {
              pricesArray.push(
                this.fb.group({
                  variantId: [price.variantId, Validators.required],
                  variantName: [price.variantName],
                  salePrice: [price.salePrice ? price.salePrice.toString() : '', [Validators.required, Validators.min(0)]],
                  discountPrice: [price.discountPrice ? price.discountPrice.toString() : '', [Validators.required, Validators.min(0), this.discountNotGreaterThanSaleValidator.bind(this)]]
                })
              );
            });
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error loading variant prices:', err);
        }
      });

      // Load initial filled cylinders from customer ledger (INITIAL_STOCK entries)
      this.ledgerService.getLedgerByCustomer(customer.id).subscribe({
        next: (ledgerEntries: any[]) => {
          // Get INITIAL_STOCK entries to populate filled cylinders
          const initialStockEntries = ledgerEntries.filter(entry => entry.refType === 'INITIAL_STOCK');
          
          // Store in map for later use when rebuilding array
          this.initialStockEntriesMap = {};
          initialStockEntries.forEach(entry => {
            this.initialStockEntriesMap[entry.variantId] = entry;
          });
          
          // Rebuild to populate with initial values
          this.buildVariantFilledCylindersArray(customer.configuredVariants || []);
          
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
          this.buildVariantFilledCylindersArray(customer.configuredVariants || []);
        }
      });
    }
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
    variantPrices.forEach(priceData => {
      const salePrice = priceData.salePrice ? parseFloat(priceData.salePrice) : null;
      const discountPrice = priceData.discountPrice ? parseFloat(priceData.discountPrice) : null;
      
      // Both prices are required
      if (salePrice !== null && discountPrice !== null) {
        const payload: any = {
          customerId: customerId,
          variantId: priceData.variantId,
          variantName: priceData.variantName,
          salePrice: salePrice,
          discountPrice: discountPrice
        };

        console.log('Saving variant pricing:', payload);

        if (isNewCustomer) {
          // For new customers, directly create
          this.variantPriceService.createPrice(customerId, payload)
            .subscribe({
              next: (response: any) => {
                console.log('Variant pricing created successfully for variant', priceData.variantId);
              },
              error: (error: any) => {
                console.error('Error creating variant pricing:', error);
              }
            });
        } else {
          // For existing customers, try UPDATE first (more likely case), then CREATE
          this.variantPriceService.updatePrice(customerId, priceData.variantId, payload)
            .subscribe({
              next: (response: any) => {
                console.log('Variant pricing updated successfully for variant', priceData.variantId);
              },
              error: (error: any) => {
                console.log('Update failed with status:', error?.status);
                
                // If update fails (404 means not found), try to create
                if (error?.status === 404) {
                  console.log('Pricing not found, attempting to create for variant', priceData.variantId);
                  this.variantPriceService.createPrice(customerId, payload)
                    .subscribe({
                      next: (response: any) => {
                        console.log('Variant pricing created successfully for variant', priceData.variantId);
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
    this.paymentForm = {
      amount: null,
      paymentDate: new Date().toISOString().split('T')[0],
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
    this.paymentError = '';

    if (!this.paymentForm.amount || this.paymentForm.amount <= 0) {
      this.paymentError = 'Please enter a valid payment amount';
      return;
    }

    if (!this.paymentForm.paymentDate) {
      this.paymentError = 'Please select a payment date';
      return;
    }

    if (!this.paymentForm.paymentMode) {
      this.paymentError = 'Please select a payment mode';
      return;
    }

    // If payment mode is not Cash, bank account is required
    if (this.paymentForm.paymentMode && this.paymentForm.paymentMode.toUpperCase() !== 'CASH' && !this.paymentForm.bankAccountId) {
      this.paymentError = 'Please select a bank account for non-cash payments';
      return;
    }

    // Get the current due amount from the most recent ledger entry
    const currentDue = this.ledgerEntries.length > 0 
      ? this.ledgerEntries[this.ledgerEntries.length - 1].dueAmount || 0 
      : 0;

    // Validate payment amount doesn't exceed due amount
    if (this.paymentForm.amount > currentDue) {
      this.paymentError = `Payment amount cannot exceed due amount of ₹${currentDue.toFixed(2)}. Current payment: ₹${this.paymentForm.amount.toFixed(2)}`;
      return;
    }

    this.isSubmittingPayment = true;
    const paymentData: any = {
      customerId: this.selectedCustomer.id,
      amount: this.paymentForm.amount,
      paymentDate: this.paymentForm.paymentDate,
      paymentMode: this.paymentForm.paymentMode
    };

    // Add bankAccountId if payment is not Cash
    if (this.paymentForm.paymentMode && this.paymentForm.paymentMode.toUpperCase() !== 'CASH' && this.paymentForm.bankAccountId) {
      paymentData.bankAccountId = this.paymentForm.bankAccountId;
    }

    this.ledgerService.recordPayment(paymentData)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording payment';
          this.paymentError = errorMessage;
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
          this.ledgerService.getLedgerByCustomerPaginated(this.selectedCustomer.id, 0, 1000).subscribe({
            next: (data: any) => {
              const allEntries = data.content || data;
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
}