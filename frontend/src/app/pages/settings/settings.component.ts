import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBox, faBuilding, faEdit, faTrash, faReceipt, faWarehouse, faDatabase, faBank, faChevronDown, faChevronUp, faBell } from '@fortawesome/free-solid-svg-icons';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { AlertSettingsService } from '../../services/alert-settings.service';
import { ToastrService } from 'ngx-toastr';
import { ToastrModule } from 'ngx-toastr';
import { BusinessInfoService } from '../../services/business-info.service';
import { BusinessInfo } from '../../models/business-info.model';
import { AlertConfig } from '../../models/alert-config.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { IndianCurrencyPipe } from 'src/app/shared/indian-currency.pipe';
import { SharedModule } from 'src/app/shared/shared.module';
import { ExpenseCategoryManagementComponent } from './expense-category-management.component';
import { WarehouseManagementComponent } from './warehouse-management.component';
import { WarehouseInventorySetupComponent } from './warehouse-inventory-setup.component';
import { BankAccountManagementComponent } from './bank-account-management.component';
import { PaymentModeManagementComponent } from './payment-mode-management.component';

interface SettingsTab {
  id: string;
  name: string;
  icon: IconDefinition;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, FontAwesomeModule, ToastrModule, SharedModule, ExpenseCategoryManagementComponent, WarehouseManagementComponent, WarehouseInventorySetupComponent, BankAccountManagementComponent, PaymentModeManagementComponent],
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
  editingVariantId: number | null = null;

  // Font Awesome Icons
  faBox = faBox;
  faBuilding = faBuilding;
  faPencil = faEdit; // Renamed to faEdit for consistency, keeping variable name for compatibility
  faTrash = faTrash;
  faReceipt = faReceipt;
  faWarehouse = faWarehouse;
  faDatabase = faDatabase;
  faBank = faBank;
  faBell = faBell;
  faChevronDown = faChevronDown;
  faChevronUp = faChevronUp;

  // Accordion state
  expandedSections: string[] = ['variants']; // Default expand variants

  tabs: SettingsTab[] = [
    { id: 'variants', name: 'Variants', icon: faBox },
    { id: 'warehouses', name: 'Warehouses', icon: faWarehouse },
    { id: 'inventory', name: 'Inventory Setup', icon: faDatabase },
    { id: 'categories', name: 'Expense Categories', icon: faReceipt },
    { id: 'payment-modes', name: 'Payment Modes', icon: faReceipt },
    { id: 'bank-accounts', name: 'Bank Accounts', icon: faBank },
    { id: 'alerts', name: 'Alerts', icon: faBell },
    { id: 'business', name: 'Business', icon: faBuilding }
  ];

  variantsList: CylinderVariant[] = [];
  alertSettingsForm!: FormGroup;
  alertSaving = false;
  alertSuccessMessage = '';
  alertErrorMessage = '';


  constructor(
    private fb: FormBuilder,
    private variantService: CylinderVariantService,
    private alertSettingsService: AlertSettingsService,
    private toastr: ToastrService,
    private businessInfoService: BusinessInfoService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForms();
  }


  ngOnInit() {
    this.loadVariants();
    this.loadBusinessInfo();
    this.loadAlertSettings();
  }

  ngOnDestroy() {}

  // Accordion toggle method - only one section open at a time
  toggleSection(sectionId: string): void {
    const index = this.expandedSections.indexOf(sectionId);
    if (index > -1) {
      this.expandedSections.splice(index, 1); // Remove if exists (collapse)
    } else {
      this.expandedSections = [sectionId]; // Close all others and open this one
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
      error: (error: unknown) => {
        // Only show a clean error message in a popup
        const err = error as { error?: { message?: string } };
        const errorMsg = typeof err?.error?.message === 'string' && !err?.error?.message.includes('<')
          ? err.error.message
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
        this.variantsList = (data.items || data);
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
      basePrice: ['', [Validators.required, Validators.min(0), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
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

    this.alertSettingsForm = this.fb.group({
      lowStockEnabled: [false],
      filledThreshold: [null, [Validators.min(0)]],
      emptyThreshold: [null, [Validators.min(0)]],
      pendingReturnEnabled: [false],
      pendingReturnThreshold: [null, [Validators.min(0)]]
    });
  }

  openVariantForm() {
    this.editingVariantId = null;
    this.variantForm.reset({ weightKg: '', active: 'true' });
    this.showVariantForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  editVariant(variant: CylinderVariant) {
    this.editingVariantId = variant.id ?? null;
    this.variantForm.patchValue({
      ...variant,
      active: (variant.active !== false).toString()
    });
    this.showVariantForm = true;
    document.querySelector('.content-wrapper')?.classList.add('modal-open');
    this.cdr.markForCheck();
  }

  saveVariant() {
    if (this.variantForm.valid) {
      const formValue = { 
        ...this.variantForm.value,
        active: this.variantForm.value.active === 'true' || this.variantForm.value.active === true
      };
      // Ensure weightKg is a number and > 0
      formValue.weightKg = parseFloat(formValue.weightKg);
      if (formValue.weightKg <= 0) {
        return;
      }

      if (this.editingVariantId) {
        const id = this.editingVariantId;
        this.variantService.updateVariant(id, formValue).subscribe({
          next: () => {
            this.toastr.success('Variant updated successfully', 'Success');
            this.variantService.invalidateCache();
            this.loadVariants();
            this.closeVariantForm();
          },
          error: (error) => {
            const errorMessage = error?.error?.message || error?.message || 'Failed to update variant';
            this.toastr.error(errorMessage, 'Error');
          }
        });
      } else {
        this.variantService.createVariant(formValue).subscribe({
          next: () => {
            this.toastr.success('Variant created successfully', 'Success');
            this.variantService.invalidateCache();
            this.loadVariants();
            this.closeVariantForm();
          },
          error: (error) => {
            const errorMessage = error?.error?.message || error?.message || 'Failed to create variant';
            this.toastr.error(errorMessage, 'Error');
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
          this.variantService.invalidateCache();
          this.loadVariants();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Failed to delete variant';
          this.toastr.error(errorMessage, 'Error');
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

  private loadAlertSettings(): void {
    this.alertErrorMessage = '';
    this.alertSuccessMessage = '';
    
    // Fetch from server
    this.alertSettingsService.getAlertConfigurations().subscribe(
      (response: AlertConfig[]) => {
        if (response && Array.isArray(response)) {
          this.mapConfigsToForm(response);
        }
        this.cdr.markForCheck();
      },
      (error: unknown) => {
        this.cdr.markForCheck();
      }
    );
  }

  private mapConfigsToForm(configs: AlertConfig[]): void {
    configs.forEach(config => {
      if (config.alertType === 'LOW_STOCK_WAREHOUSE') {
        this.alertSettingsForm.patchValue({
          lowStockEnabled: config.enabled,
          filledThreshold: config.filledCylinderThreshold || 50,
          emptyThreshold: config.emptyCylinderThreshold || 50
        });
      }
      if (config.alertType === 'PENDING_RETURN_CYLINDERS') {
        this.alertSettingsForm.patchValue({
          pendingReturnEnabled: config.enabled,
          pendingReturnThreshold: config.pendingReturnThreshold || 10
        });
      }
    });
    this.alertSettingsForm.markAsPristine();
  }

  saveAlertSettings(): void {
    const formValue = this.alertSettingsForm.value;

    // Validate enabled alerts have threshold values
    if (formValue.lowStockEnabled) {
      if (!formValue.filledThreshold && formValue.filledThreshold !== 0) {
        this.toastr.error('Please enter Filled Cylinders Threshold for Low Stock Alert', 'Validation Error');
        return;
      }
      if (!formValue.emptyThreshold && formValue.emptyThreshold !== 0) {
        this.toastr.error('Please enter Empty Cylinders Threshold for Low Stock Alert', 'Validation Error');
        return;
      }
    }

    if (formValue.pendingReturnEnabled) {
      if (!formValue.pendingReturnThreshold && formValue.pendingReturnThreshold !== 0) {
        this.toastr.error('Please enter Cylinders Pending Threshold for Pending Returns Alert', 'Validation Error');
        return;
      }
    }

    this.alertSaving = true;

    const lowStockPayload: Partial<AlertConfig> = {
      enabled: formValue.lowStockEnabled
    };
    if (formValue.lowStockEnabled) {
      lowStockPayload.filledCylinderThreshold = formValue.filledThreshold;
      lowStockPayload.emptyCylinderThreshold = formValue.emptyThreshold;
    }

    this.alertSettingsService.updateAlertConfig('LOW_STOCK_WAREHOUSE', lowStockPayload).subscribe(
      () => {
        const pendingReturnPayload: Partial<AlertConfig> = {
          enabled: formValue.pendingReturnEnabled
        };
        if (formValue.pendingReturnEnabled) {
          pendingReturnPayload.pendingReturnThreshold = formValue.pendingReturnThreshold;
        }

        this.alertSettingsService.updateAlertConfig('PENDING_RETURN_CYLINDERS', pendingReturnPayload).subscribe(
          () => {
            this.alertSaving = false;
            this.alertSettingsForm.markAsPristine();
            this.toastr.success('Alert settings saved successfully', 'Success');
            this.cdr.markForCheck();
          },
          (error: unknown) => {
            this.alertSaving = false;
            this.toastr.error('Failed to save pending returns alert settings', 'Error');
            this.cdr.markForCheck();
          }
        );
      },
      (error: unknown) => {
        this.alertSaving = false;
        this.toastr.error('Failed to save low stock alert settings', 'Error');
        this.cdr.markForCheck();
      }
    );
  }
}
