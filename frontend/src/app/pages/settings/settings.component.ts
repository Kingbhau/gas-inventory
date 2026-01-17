import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBox, faBuilding, faPencil, faTrash, faReceipt, faWarehouse, faDatabase, faBank, faChevronDown, faChevronUp } from '@fortawesome/free-solid-svg-icons';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { ToastrService } from 'ngx-toastr';
import { ToastrModule } from 'ngx-toastr';
import { BusinessInfoService, BusinessInfo } from '../../services/business-info.service';
import { IndianCurrencyPipe } from 'src/app/shared/indian-currency.pipe';
import { SharedModule } from 'src/app/shared/shared.module';
import { ExpenseCategoryManagementComponent } from './expense-category-management.component';
import { WarehouseManagementComponent } from './warehouse-management.component';
import { WarehouseInventorySetupComponent } from './warehouse-inventory-setup.component';
import { BankAccountManagementComponent } from './bank-account-management.component';

interface SettingsTab {
  id: string;
  name: string;
  icon: any;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, FontAwesomeModule, ToastrModule, SharedModule, ExpenseCategoryManagementComponent, WarehouseManagementComponent, WarehouseInventorySetupComponent, BankAccountManagementComponent],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent implements OnInit, OnDestroy {
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

  activeTab = 'variants';
  variantForm!: FormGroup;
  businessForm!: FormGroup;
  businessLoading = false;
  businessError: string | null = null;

  showVariantForm = false;
  editingVariantId: string | null = null;

  // Font Awesome Icons
  faBox = faBox;
  faBuilding = faBuilding;
  faPencil = faPencil;
  faTrash = faTrash;
  faReceipt = faReceipt;
  faWarehouse = faWarehouse;
  faDatabase = faDatabase;
  faBank = faBank;
  faChevronDown = faChevronDown;
  faChevronUp = faChevronUp;

  // Accordion state
  expandedSections: string[] = ['variants']; // Default expand variants

  tabs: SettingsTab[] = [
    { id: 'variants', name: 'Variants', icon: faBox },
    { id: 'warehouses', name: 'Warehouses', icon: faWarehouse },
    { id: 'inventory', name: 'Inventory Setup', icon: faDatabase },
    { id: 'categories', name: 'Expense Categories', icon: faReceipt },
    { id: 'bank-accounts', name: 'Bank Accounts', icon: faBank },
    { id: 'business', name: 'Business', icon: faBuilding }
  ];

  variantsList: any[] = [];


  constructor(
    private fb: FormBuilder,
    private variantService: CylinderVariantService,
    private toastr: ToastrService,
    private businessInfoService: BusinessInfoService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForms();
  }


  ngOnInit() {
    this.loadVariants();
    this.loadBusinessInfo();
  }

  ngOnDestroy() {}

  // Accordion toggle method
  toggleSection(sectionId: string): void {
    const index = this.expandedSections.indexOf(sectionId);
    if (index > -1) {
      this.expandedSections.splice(index, 1); // Remove if exists (collapse)
    } else {
      this.expandedSections.push(sectionId); // Add if not exists (expand)
    }
    this.cdr.markForCheck();
  }

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

  initForms() {
    this.variantForm = this.fb.group({
      name: ['', Validators.required],
      weightKg: ['', [Validators.required, Validators.min(0.1), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      active: [true]
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
