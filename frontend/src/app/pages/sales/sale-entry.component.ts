import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { catchError, of } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { SharedModule } from '../../shared/shared.module';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { SaleService } from '../../services/sale.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-balance.service';
import { MonthlyPriceService } from '../../services/monthly-price.service';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-sale-entry',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './sale-entry.component.html',
  styleUrl: './sale-entry.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaleEntryComponent implements OnInit, OnDestroy {
  saleForm!: FormGroup;
  successMessage = '';
  baseAmount = 0;
  isSubmitting = false;

  displayCustomerName(customer: any): string {
    return customer && typeof customer === 'object' ? customer.name : '';
  }

  displayVariantName(variant: any): string {
    return variant && typeof variant === 'object' ? variant.name : '';
  }

  // Font Awesome Icons
  faCheckCircle = faCheckCircle;

  customers: any[] = [];
  variants: any[] = [];
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
    private monthlyPriceService: MonthlyPriceService,
    private toastr: ToastrService,
    private customerCylinderLedgerService: CustomerCylinderLedgerService
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadCustomers();
    this.loadVariants();
    this.filteredCustomers = this.customers;
    this.filteredVariants = this.variants;
    this.saleForm.get('variantId')?.valueChanges.subscribe(variantObj => {
      this.prefillBasePrice(variantObj && variantObj.id ? variantObj.id : null);
    });
  }

  ngOnDestroy() {}

  prefillBasePrice(variantId: number) {
    if (!variantId) {
      this.saleForm.get('basePrice')?.setValue(0);
      return;
    }
    const now = new Date();
    const monthYear = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}`;
    this.monthlyPriceService.getPriceForMonth(variantId, monthYear).subscribe({
      next: (price) => {
        this.saleForm.get('basePrice')?.setValue(price.basePrice);
        this.saleForm.get('basePrice')?.disable();
        this.calculateTotal();
      },
      error: () => {
        this.saleForm.get('basePrice')?.setValue(0);
        this.saleForm.get('basePrice')?.disable();
        this.calculateTotal();
      }
    });
  }

  onCustomerSearchChange() {
    const search = this.customerSearch.toLowerCase();
    this.filteredCustomers = this.customers.filter(c => c.name.toLowerCase().includes(search));
  }

  onVariantSearchChange() {
    const search = this.variantSearch.toLowerCase();
    this.filteredVariants = this.variants.filter(v => v.name.toLowerCase().includes(search));
  }

  loadCustomers() {
    this.customerService.getAllCustomers(0, 100).subscribe({
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
    this.variantService.getAllVariants(0, 100).subscribe({
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

  initForm() {
    this.saleForm = this.fb.group({
      customerId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledIssuedQty: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      emptyReceivedQty: [null, [Validators.min(0), Validators.max(100)]],
      basePrice: [null, [Validators.required, Validators.min(0), Validators.max(10000)]],
      discount: [null, [Validators.min(0), Validators.max(10000)]]
    });
  }

  calculateTotal() {
    const filledQty = Number(this.saleForm.get('filledIssuedQty')?.value) || 0;
    const basePrice = Number(this.saleForm.get('basePrice')?.value) || 0;
    // Prevent negative or non-integer quantities/prices
    this.baseAmount = Math.max(0, filledQty) * Math.max(0, basePrice);
    // Prevent negative baseAmount
    if (this.baseAmount < 0 || isNaN(this.baseAmount)) this.baseAmount = 0;
  }

  get totalAmount(): number {
    const discount = Number(this.saleForm.get('discount')?.value) || 0;
    let total = this.baseAmount - Math.max(0, discount);
    // Prevent negative totals or discounts greater than baseAmount
    if (total < 0) total = 0;
    return total;
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
    // Prevent double submission
    if (this.isSubmitting) {
      return;
    }

    const customerObj = this.saleForm.get('customerId')?.value;
    const variantObj = this.saleForm.get('variantId')?.value;
    const saleRequest = {
      customerId: customerObj && customerObj.id ? customerObj.id : null,
      items: [
        {
          variantId: variantObj && variantObj.id ? variantObj.id : null,
          qtyIssued: parseInt(this.saleForm.get('filledIssuedQty')?.value),
          qtyEmptyReceived: parseInt(this.saleForm.get('emptyReceivedQty')?.value),
          discount: parseFloat(this.saleForm.get('discount')?.value) || 0
        }
      ]
    };

    this.isSubmitting = true;

    const sub = this.saleService.createSale(saleRequest)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error recording sale. Please try again.';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of(null);
        })
      )
      .subscribe(response => {
        if (response) {
          this.toastr.success('Sale recorded successfully', 'Success');
          this.resetForm();
        }
        this.isSubmitting = false;
        sub.unsubscribe();
      });
  }

  resetForm() {
    this.saleForm.reset({ customerId: null, variantId: null, filledIssuedQty: null, emptyReceivedQty: null, basePrice: null, discount: null });
    this.saleForm.get('basePrice')?.disable();
    this.baseAmount = 0;
  }
}
