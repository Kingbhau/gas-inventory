import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule, FormControl } from '@angular/forms';
import { Component, OnInit, ViewChild, ViewChildren, QueryList } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { startWith, map, finalize } from 'rxjs';
import { catchError, of } from 'rxjs';
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
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-balance.service';
import { MonthlyPriceService } from '../../services/monthly-price.service';
import { CustomerVariantPriceService } from '../../services/customer-variant-price.service';
import { LoadingService } from '../../services/loading.service';
import { WarehouseService } from '../../services/warehouse.service';
import { BankAccount } from '../../models/bank-account.model';

@Component({
  selector: 'app-sale-entry',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule, MatOptionModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './sale-entry.component.html',
  styleUrl: './sale-entry.component.css'
})
export class SaleEntryComponent implements OnInit {
  @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
  
  saleForm!: FormGroup;
  successMessage = '';
  baseAmount = 0;
  discountPrice: number = 0; // Customer-specific discount price

  customerIdControl = new FormControl(null, Validators.required);
  variantIdControl = new FormControl(null, Validators.required);
  customerDropdownConfig: any = {
    displayKey: 'name',
    search: true,
    placeholder: 'Select a customer',
    noResultsFound: 'No customer found',
    searchPlaceholder: 'Search customer...',
    height: '200px',
    customComparator: undefined,
    limitTo: 100
  };
  variantDropdownConfig: any = {
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

  customers: any[] = [];
  variants: any[] = [];
  warehouses: any[] = [];
  bankAccounts: BankAccount[] = [];
  filteredCustomers: any[] = [];
  filteredVariants: any[] = [];
  customerSearch: string = '';
  variantSearch: string = '';
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
    private warehouseService: WarehouseService
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadCustomers();
    this.loadVariants();
    this.loadWarehouses();
    this.filteredCustomers = this.customers;
    this.filteredVariants = this.variants;
    
    // When customer changes, filter variants and prefill prices
    this.saleForm.get('customerId')?.valueChanges.subscribe(customerObj => {
      const customerId = customerObj && customerObj.id ? customerObj.id : null;
      this.filterVariantsByCustomerConfig(customerId);
      // Also check if variant is already selected to prefill prices
      const variantObj = this.saleForm.get('variantId')?.value;
      if (variantObj && variantObj.id) {
        this.prefillPrice(customerId, variantObj.id);
      }
    });
    
    // When variant changes, prefill prices for the selected customer
    this.saleForm.get('variantId')?.valueChanges.subscribe(variantObj => {
      const variantId = variantObj && variantObj.id ? variantObj.id : null;
      const customerObj = this.saleForm.get('customerId')?.value;
      const customerId = customerObj && customerObj.id ? customerObj.id : null;
      this.prefillPrice(customerId, variantId);
    });
  }

  displayCustomerName(customer: any): string {
    return customer && typeof customer === 'object' ? customer.name : '';
  }

  displayVariantName(variant: any): string {
    return variant && typeof variant === 'object' ? variant.name : '';
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
    if (!customer || !customer.configuredVariants || customer.configuredVariants.length === 0) {
      this.filteredVariants = this.variants;
      return;
    }
    
    // Filter variants to only show those configured for this customer
    this.filteredVariants = this.variants.filter(v => 
      customer.configuredVariants.includes(v.id)
    );
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
      next: (response: any) => {
        if (response && response.data) {
          // Use customer-specific sale price and discount price
          this.saleForm.get('basePrice')?.setValue(response.data.salePrice);
          this.discountPrice = response.data.discountPrice || 0;
          this.saleForm.get('basePrice')?.disable();
          this.calculateTotal();
          console.log('Using customer-specific pricing - Sale:', response.data.salePrice, 'Discount:', this.discountPrice);
        } else {
          // Fall back to monthly pricing
          this.prefillBasePrice(variantId);
        }
      },
      error: () => {
        // Fall back to monthly pricing if customer pricing not found
        console.log('Customer-specific pricing not found, using monthly pricing');
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
    this.loadingService.show('Loading customers...');
    this.customerService.getAllCustomers(0, 100)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.customers = data.content || data;
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading customers';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadVariants() {
    this.loadingService.show('Loading variants...');
    this.variantService.getAllVariants(0, 100)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.variants = data.content || data;
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading variants';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadWarehouses() {
    this.warehouseService.getActiveWarehouses()
      .subscribe({
        next: (response: any) => {
          if (response.success) {
            this.warehouses = response.data || [];
          } else {
            this.toastr.error('Failed to load warehouses', 'Error');
          }
        },
        error: (error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading warehouses';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
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

  initForm() {
    this.saleForm = this.fb.group({
      warehouseId: [null, Validators.required],
      customerId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledIssuedQty: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      emptyReceivedQty: [null, [Validators.min(0), Validators.max(100)]],
      basePrice: [null, [Validators.required, Validators.min(0), Validators.max(10000)]],
      amountReceived: [null, [Validators.min(0), Validators.max(100000)]],
      modeOfPayment: [null],
      bankAccountId: [null]
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
    });

    // When modeOfPayment changes, show/hide bank account field
    this.saleForm.get('modeOfPayment')?.valueChanges.subscribe((mode) => {
      const bankAccountControl = this.saleForm.get('bankAccountId');
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
  onWarehouseSelected(warehouse: any): void {
    if (warehouse && warehouse.id) {
      this.saleForm.get('warehouseId')?.setValue(warehouse.id);
    } else {
      this.saleForm.get('warehouseId')?.setValue(null);
    }
  }

  onCustomerSelected(customer: any): void {
    if (customer && customer.id) {
      this.saleForm.get('customerId')?.setValue(customer.id);
      this.saleForm.get('customerId')?.markAsTouched();
      // Reload prices when customer changes
      this.prefillPrice(customer.id, this.saleForm.get('variantId')?.value);
    } else {
      this.saleForm.get('customerId')?.setValue(null);
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
    }
  }

  onVariantSelected(variant: any): void {
    if (variant && variant.id) {
      this.saleForm.get('variantId')?.setValue(variant.id);
      this.saleForm.get('variantId')?.markAsTouched();
      // Reload prices when variant changes
      this.prefillPrice(this.saleForm.get('customerId')?.value, variant.id);
    } else {
      this.saleForm.get('variantId')?.setValue(null);
      this.saleForm.get('basePrice')?.setValue(0);
      this.discountPrice = 0;
    }
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
    const qtyEmptyReceived = parseInt(this.saleForm.get('emptyReceivedQty')?.value);
    // Prevent negative or non-integer empty returns
    if (qtyEmptyReceived < 0 || isNaN(qtyEmptyReceived)) {
      this.toastr.error('Empty cylinders returned must be zero or positive.', 'Validation Error');
      return;
    }
    if (qtyEmptyReceived > 0) {
      const sub = this.customerCylinderLedgerService.getCustomerVariantBalance(customerId, variantId)
        .pipe(
          catchError((err: any) => {
            const errorMessage = err?.error?.message || err?.message || 'Could not validate customer balance. Please try again.';
            this.toastr.error(errorMessage, 'Error');
            return of(null);
          })
        )
        .subscribe(balance => {
          if (balance !== null && qtyEmptyReceived > balance) {
            this.toastr.error('Cannot return more empty cylinders than the customer currently holds for this variant.', 'Validation Error');
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
    // Validate warehouse is selected
    if (!this.saleForm.get('warehouseId')?.value) {
      this.toastr.error('Please select a warehouse', 'Validation Error');
      this.saleForm.get('warehouseId')?.markAsTouched();
      return;
    }

    // Validate form is valid
    if (this.saleForm.invalid) {
      this.toastr.error('Please fill all required fields', 'Validation Error');
      return;
    }

    const customerId = this.saleForm.get('customerId')?.value;
    const variantId = this.saleForm.get('variantId')?.value;
    const warehouseId = this.saleForm.get('warehouseId')?.value;
    const qtyIssued = parseInt(this.saleForm.get('filledIssuedQty')?.value);
    const modeOfPayment = this.saleForm.get('modeOfPayment')?.value;
    const bankAccountIdValue = this.saleForm.get('bankAccountId')?.value;
    
    // Calculate total discount (per-unit discount × quantity)
    const totalDiscount = (this.discountPrice || 0) * qtyIssued;
    
    const saleRequest: any = {
      warehouseId: warehouseId,
      customerId: customerId,
      amountReceived: this.saleForm.get('amountReceived')?.value || 0,
      modeOfPayment: modeOfPayment,
      items: [
        {
          variantId: variantId,
          qtyIssued: qtyIssued,
          qtyEmptyReceived: parseInt(this.saleForm.get('emptyReceivedQty')?.value),
          discount: totalDiscount
        }
      ]
    };

    // Add bankAccountId if payment is not CASH
    if (modeOfPayment && modeOfPayment.toUpperCase() !== 'CASH' && bankAccountIdValue) {
      saleRequest.bankAccountId = bankAccountIdValue;
    }
    const sub = this.saleService.createSale(saleRequest)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording sale. Please try again.';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of(null);
        })
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.toastr.success('Sale recorded successfully', 'Success');
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
    
    // Rebuild the form completely with all fields
    this.saleForm = this.fb.group({
      warehouseId: [null, Validators.required],
      customerId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledIssuedQty: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      emptyReceivedQty: [null, [Validators.min(0), Validators.max(100)]],
      basePrice: [null, [Validators.required, Validators.min(0), Validators.max(10000)]],
      amountReceived: [null, [Validators.required, Validators.min(0), Validators.max(100000)]],
      modeOfPayment: [null, Validators.required],
      bankAccountId: [null]
    });
    
    this.saleForm.get('basePrice')?.disable();
    this.saleForm.markAsPristine();
    this.saleForm.markAsUntouched();
    this.baseAmount = 0;
    this.discountPrice = 0;
    this.filteredVariants = this.variants;
  }
}
