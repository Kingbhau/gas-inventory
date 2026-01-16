
import { catchError, of, finalize } from 'rxjs';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators, FormArray, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faPencil, faTrash, faBook, faPlus, faTimes, faExclamation, faUsers, faEye, faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { CustomerService } from '../../services/customer.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { CustomerBalance, VariantBalance } from '../../models/customer-balance.model';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { CustomerVariantPriceService } from '../../services/customer-variant-price.service';
import { LoadingService } from '../../services/loading.service';
import { ToastrService } from 'ngx-toastr';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

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

  customers: any[] = [];
  ledgerEntries: any[] = [];
  variants: any[] = [];

  showDetailsModal = false;
  detailsCustomer: any = null;
  detailsVariantPrices: any[] = [];


  // Rename all pendingUnits to returnPendingUnits for clarity
  // Add property to store filled units
  // This will be set per customer
  //filledUnits: number = 0;

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private ledgerService: CustomerCylinderLedgerService,
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
  }

  ngOnDestroy() {}

  get filteredVariants() {
    if (!this.ledgerFilterVariant) return this.variants;
    return this.variants.filter(v => v.name === this.ledgerFilterVariant);
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
      entries = entries.filter(e => e.variantName === this.ledgerFilterVariant);
    }
    // Already sorted newest to oldest from backend (id DESC)
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
      active: [true, Validators.required],
      configuredVariants: [[], Validators.required],
      variantPrices: this.fb.array([])
    });

    // Listen for variant configuration changes
    this.customerForm.get('configuredVariants')?.valueChanges.subscribe(selectedVariantIds => {
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
  }

  editCustomer(customer: any) {
    this.showForm = true;
    this.editingId = customer.id;
    this.customerForm.patchValue({
      ...customer,
      active: customer.active !== undefined ? customer.active : true,
      configuredVariants: customer.configuredVariants || []
    });

    // Load existing variant prices from backend
    if (customer.id) {
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
    const customerData = {
      name: formValue.name,
      mobile: formValue.mobile,
      address: formValue.address,
      active: formValue.active,
      configuredVariants: formValue.configuredVariants
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
        const newEntries = (data.content || data).sort((a: any, b: any) => b.id - a.id);
        
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
}
