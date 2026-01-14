import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBox, faDollarSign, faBuilding, faPencil, faTrash, faReceipt } from '@fortawesome/free-solid-svg-icons';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { MonthlyPriceService } from '../../services/monthly-price.service';
import { ToastrService } from 'ngx-toastr';
import { ToastrModule } from 'ngx-toastr';
import { BusinessInfoService, BusinessInfo } from '../../services/business-info.service';
import { IndianCurrencyPipe } from 'src/app/shared/indian-currency.pipe';
import { SharedModule } from 'src/app/shared/shared.module';
import { ExpenseCategoryManagementComponent } from './expense-category-management.component';

interface SettingsTab {
  id: string;
  name: string;
  icon: any;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, FontAwesomeModule, ToastrModule, SharedModule, ExpenseCategoryManagementComponent],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent implements OnInit, OnDestroy {
      // Pagination state for price section
      pricePage = 1;
      pricePageSize = 10;
      totalPrices = 0;
      priceTotalPages = 1;

  get paginatedMonthlyPrices() {
    // Paginate the grouped months array
    const start = (this.pricePage - 1) * this.pricePageSize;
    const end = start + this.pricePageSize;
    return this.monthlyPrices.slice(start, end);
  }
    // Pagination state for variants table
    variantPage = 1;
    variantPageSize = 10;
    totalVariants = 0;
    variantTotalPages = 1;

    get paginatedVariants() {
      // Already paged from backend
      return this.variantsList;
    }
    getVariantTotalPages() {
      return this.variantTotalPages;
    }
    // getPriceTotalPages() is now backend-driven, see above
  activeTab = 'variants';
  variantForm!: FormGroup;
  priceForm!: FormGroup;
  businessForm!: FormGroup;
  businessLoading = false;
  businessError: string | null = null;

  showVariantForm = false;
  showPriceForm = false;
  editingVariantId: string | null = null;
  editingPrice: any = null;
  isEditingPrice = false; // Track if we're editing a price
  selectedMonth: string = ''; // Store selected month in YYYY-MM format

  // Font Awesome Icons
  faBox = faBox;
  faDollarSign = faDollarSign;
  faBuilding = faBuilding;
  faPencil = faPencil;
  faTrash = faTrash;
  faReceipt = faReceipt;

  tabs: SettingsTab[] = [
    { id: 'variants', name: 'Variants', icon: faBox },
    { id: 'pricing', name: 'Pricing', icon: faDollarSign },
    { id: 'categories', name: 'Expense Categories', icon: faReceipt },
    { id: 'business', name: 'Business', icon: faBuilding }
  ];

  variantsList: any[] = [];
  monthlyPrices: any[] = [];


  constructor(
    private fb: FormBuilder,
    private variantService: CylinderVariantService,
    private priceService: MonthlyPriceService,
    private toastr: ToastrService,
    private businessInfoService: BusinessInfoService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForms();
  }


  ngOnInit() {
    this.loadVariants();
    this.loadPrices();
    this.loadBusinessInfo();
  }

  ngOnDestroy() {}

  loadBusinessInfo() {
    this.businessLoading = true;
    this.businessError = null;
    // Fetch business info by default ID 1
    this.businessInfoService.getBusinessInfoById(1).subscribe({
      next: (data: BusinessInfo) => {
        this.businessForm.patchValue(data || {});
        this.businessLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        // Only show a clean error message in a popup
        const errorMsg = typeof error?.error?.message === 'string' && !error?.error?.message.includes('<')
          ? error.error.message
          : 'Failed to load business info';
        this.toastr.error(errorMsg, 'Error');
        this.businessLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadVariants() {
    this.variantService.getAllVariants(this.variantPage - 1, this.variantPageSize, 'id', 'DESC').subscribe({
      next: (data) => {
        this.variantsList = (data.content || data);
        if (!Array.isArray(this.variantsList)) {
          this.variantsList = [];
        }
        this.totalVariants = data.totalElements || this.variantsList.length;
        this.variantTotalPages = data.totalPages || 1;
        this.cdr.markForCheck();
      },
      error: (error) => {
        const errorMessage = error?.error?.message || error?.message || 'Failed to load variants';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
        this.variantsList = [];
        this.totalVariants = 0;
        this.variantTotalPages = 1;
        this.cdr.markForCheck();
      }
    });
  }

  onVariantPageChange(page: number) {
    this.variantPage = page;
    this.loadVariants();
    this.cdr.markForCheck();
  }

  onVariantPageSizeChange(size: number) {
    this.variantPageSize = size;
    this.variantPage = 1;
    this.loadVariants();
    this.cdr.markForCheck();
  }

  loadPrices() {
    this.priceService.getAllPrices(0, 10000).subscribe({
      next: (data) => {
        const priceList = (data.content || data);
        if (!Array.isArray(priceList)) {
          this.monthlyPrices = [];
          this.totalPrices = 0;
          this.priceTotalPages = 1;
          this.cdr.markForCheck();
          return;
        }
        // Group prices by monthYear
        const grouped: { [key: string]: any } = {};
        priceList.forEach((price: any) => {
          if (!grouped[price.monthYear]) {
            grouped[price.monthYear] = {
              month: price.monthYear,
              variants: []
            };
          }
          grouped[price.monthYear].variants.push({
            id: price.variantId,
            priceId: price.id, // Store the individual price record ID
            name: price.variantName,
            price: price.basePrice
          });
        });
        this.monthlyPrices = Object.values(grouped);
        this.totalPrices = this.monthlyPrices.length;
        this.priceTotalPages = Math.ceil(this.totalPrices / this.pricePageSize) || 1;
        this.cdr.markForCheck();
      },
      error: (error) => {
        const errorMessage = error?.error?.message || error?.message || 'Failed to load prices';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
        this.monthlyPrices = [];
        this.totalPrices = 0;
        this.priceTotalPages = 1;
        this.cdr.markForCheck();
      }
    });
  }

  onPricePageChange(page: number) {
    this.pricePage = page;
    this.loadPrices();
    this.cdr.markForCheck();
  }

  onPricePageSizeChange(size: number) {
    this.pricePageSize = size;
    this.pricePage = 1;
    this.loadPrices();
    this.cdr.markForCheck();
  }

  initForms() {
    this.variantForm = this.fb.group({
      name: ['', Validators.required],
      weightKg: ['', [Validators.required, Validators.min(0.1), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      active: [true]
    });

    this.priceForm = this.fb.group({
      month: ['', Validators.required],
      variants: this.fb.array([])
    });

    this.businessForm = this.fb.group({
      agencyName: ['', [Validators.required, Validators.maxLength(100)]],
      registrationNumber: ['', [Validators.maxLength(50)]],
      gstNumber: ['', [Validators.maxLength(20)]],
      address: ['', [Validators.maxLength(200)]],
      contactNumber: ['', [Validators.pattern(/^\+?[0-9]{7,15}$/)]],
      email: ['', [Validators.email, Validators.maxLength(100)]]
    });
  }

  openVariantForm() {
    this.editingVariantId = null;
    this.variantForm.reset({ weightKg: '', active: true });
    this.showVariantForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  editVariant(variant: any) {
    this.editingVariantId = variant.id;
    this.variantForm.patchValue(variant);
    this.showVariantForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  saveVariant() {
    if (this.variantForm.valid) {
      const formValue = { ...this.variantForm.value };
      // Ensure weightKg is a number and > 0
      formValue.weightKg = parseFloat(formValue.weightKg);
      if (formValue.weightKg <= 0) {
        console.error('Weight must be greater than 0');
        return;
      }

      if (this.editingVariantId) {
        const id = typeof this.editingVariantId === 'string' ? parseInt(this.editingVariantId, 10) : this.editingVariantId;
        this.variantService.updateVariant(id, formValue).subscribe({
          next: () => {
            this.toastr.success('Variant updated successfully', 'Success');
            this.loadVariants();
            this.closeVariantForm();
          },
          error: (error) => {
            const errorMessage = error?.error?.message || error?.message || 'Failed to update variant';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
          }
        });
      } else {
        this.variantService.createVariant(formValue).subscribe({
          next: () => {
            this.toastr.success('Variant created successfully', 'Success');
            this.loadVariants();
            this.closeVariantForm();
          },
          error: (error) => {
            const errorMessage = error?.error?.message || error?.message || 'Failed to create variant';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
          }
        });
      }
    }
  }

  deleteVariant(id: string) {
    if (confirm('Delete this variant?')) {
      const numId = typeof id === 'string' ? parseInt(id, 10) : id;
      this.variantService.deleteVariant(numId).subscribe({
        next: () => {
          this.toastr.success('Variant deleted successfully', 'Success');
          this.loadVariants();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Failed to delete variant';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
    }
  }

  closeVariantForm() {
    this.showVariantForm = false;
    this.editingVariantId = null;
    document.querySelector('.content-wrapper')?.classList.remove('modal-open');
    this.cdr.markForCheck();
  }

  openPriceForm() {
    const today = new Date();
    const dateString = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`;
    const monthString = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`; // YYYY-MM format for input
    
    // Find existing prices for this month using the SAME format
    const existingPricesThisMonth = this.monthlyPrices
      .find((m: any) => m.month === dateString)?.variants || [];
    const existingVariantIds = new Set(existingPricesThisMonth.map((v: any) => v.id));
    
    // Only show variants that don't already have a price for this month
    const availableVariants = this.variantsList
      .filter((v: any) => !existingVariantIds.has(v.id))
      .map(v => ({
        id: v.id,
        name: v.name,
        price: 0
      }));
    
    this.selectedMonth = monthString; // Set initial month value
    
    this.editingPrice = {
      id: null,
      monthYear: dateString,
      month: dateString,
      variants: availableVariants
    };
    this.isEditingPrice = false; // We're adding new prices
    this.priceForm.patchValue({
      month: this.editingPrice.month
    });
    this.showPriceForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  editPrice(price: any) {
    // Extract month from the price month field (format: "2026-01-01")
    const monthParts = price.month.split('-');
    const monthString = `${monthParts[0]}-${monthParts[1]}`; // Convert to "2026-01" format
    const dateString = `${monthParts[0]}-${monthParts[1]}-01`; // Ensure full date format "2026-01-01"
    
    this.editingPrice = JSON.parse(JSON.stringify(price));
    this.isEditingPrice = true; // We're editing existing prices
    this.selectedMonth = monthString; // Set the calendar to show the month
    
    // Ensure monthYear is set in the proper format
    this.editingPrice.monthYear = dateString;
    this.editingPrice.month = dateString;
    
    // For edit mode, ensure variants are available
    if (!this.editingPrice.variants || this.editingPrice.variants.length === 0) {
      this.editingPrice.variants = [];
    }
    
    this.priceForm.patchValue({
      month: this.editingPrice.month,
      variants: this.editingPrice.variants
    });
    this.showPriceForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  onMonthChange(monthValue: string) {
    if (!monthValue) {
      return;
    }

    // monthValue format: "2026-01"
    const dateString = `${monthValue}-01`; // Convert to "2026-01-01"
    
    // Find existing prices for this month
    const existingPricesThisMonth = this.monthlyPrices
      .find((m: any) => m.month === dateString)?.variants || [];
    const existingVariantIds = new Set(existingPricesThisMonth.map((v: any) => v.id));
    
    // Only show variants that don't already have a price for this month
    const availableVariants = this.variantsList
      .filter((v: any) => !existingVariantIds.has(v.id))
      .map(v => ({
        id: v.id,
        name: v.name,
        price: 0
      }));
    
    // Update editingPrice with selected month
    this.editingPrice = {
      id: null,
      monthYear: dateString,
      month: dateString,
      variants: availableVariants
    };
    this.cdr.markForCheck();
  }

  savePrices() {
    if (!this.editingPrice || !this.editingPrice.variants) {
      return;
    }

    // Ensure monthYear is properly set - use monthYear if available, otherwise use month or construct from selectedMonth
    let monthYear = this.editingPrice.monthYear;
    
    if (!monthYear) {
      // Fallback: construct from selectedMonth or editingPrice.month
      const monthStr = this.editingPrice.month || this.selectedMonth;
      if (monthStr && monthStr.length >= 7) {
        monthYear = `${monthStr.substring(0, 7)}-01`; // Ensure "YYYY-MM-01" format
      }
    }
    
    if (!monthYear) {
      this.toastr.error('Month is required. Please select a month and try again.', 'Error');
      return;
    }
    
    // Only check for duplicates if we're adding new prices (not editing)
    if (!this.isEditingPrice) {
      // Check for duplicates in the variants being saved
      const pricesByVariant = new Set<number>();
      const duplicateVariants: string[] = [];
      
      for (const variant of this.editingPrice.variants) {
        if (!variant.price) continue;
        
        if (pricesByVariant.has(variant.id)) {
          duplicateVariants.push(variant.name);
        }
        pricesByVariant.add(variant.id);
        
        // Also check against existing prices
        const existingForThisMonth = this.monthlyPrices
          .find((m: any) => m.month === this.editingPrice.month);
        
        if (existingForThisMonth) {
          const alreadyExists = existingForThisMonth.variants
            .some((v: any) => v.id === variant.id);
          
          if (alreadyExists) {
            duplicateVariants.push(variant.name);
          }
        }
      }
      
      // Show error if duplicates found
      if (duplicateVariants.length > 0) {
        this.toastr.error(`Price already exists for: ${duplicateVariants.join(', ')} in ${this.editingPrice.month}`, 'Duplicate Price');
        return;
      }
    }

    // Create individual MonthlyPrice entries for each variant
    const pricePromises = this.editingPrice.variants.map((variant: any) => {
      if (!variant.price) {
        return Promise.resolve();
      }

      const priceData: any = {
        variantId: variant.id,
        variantName: variant.name,
        monthYear: monthYear,
        basePrice: parseFloat(variant.price),
        createdAt: new Date().toISOString().split('T')[0]
      };

      // When editing, use variant.priceId if available; otherwise use variant.id
      // When adding new, priceId won't exist, so create new
      const priceId = variant.priceId || variant.id;
      
      if (this.isEditingPrice && variant.priceId) {
        // Update existing price using the priceId
        return new Promise((resolve, reject) => {
          this.priceService.updatePrice(variant.priceId, priceData).subscribe({
            next: () => resolve(true),
            error: (err) => reject(err)
          });
        });
      } else {
        // Create new price
        return new Promise((resolve, reject) => {
          this.priceService.createPrice(priceData).subscribe({
            next: () => resolve(true),
            error: (err) => reject(err)
          });
        });
      }
    });

    Promise.all(pricePromises)
      .then(() => {
        this.toastr.success('Prices saved successfully', 'Success');
        this.loadPrices();
        this.closePriceForm();
      })
      .catch((error) => {
        const errorMessage = error?.error?.message || error?.message || 'Error saving prices. Please try again.';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
      });
  }

  closePriceForm() {
    this.showPriceForm = false;
    this.editingPrice = null;
    this.isEditingPrice = false; // Reset the flag
    document.querySelector('.content-wrapper')?.classList.remove('modal-open');
  }

  saveBusinessInfo() {
    if (this.businessForm.invalid) {
      this.businessForm.markAllAsTouched();
      this.toastr.warning('Please fill in all required fields correctly', 'Validation Error');
      return;
    }
    this.businessLoading = true;
    this.businessError = null;
    const data: BusinessInfo = this.businessForm.value;
    this.businessInfoService.createBusinessInfo(data).subscribe({
      next: (resp) => {
        this.toastr.success('Business information saved successfully', 'Success');
        this.businessLoading = false;
      },
      error: (error) => {
        this.businessError = error?.error?.message || error?.message || 'Failed to save business info';
        this.toastr.error(this.businessError ?? '', 'Error');
        this.businessLoading = false;
      }
    });
  }
}
