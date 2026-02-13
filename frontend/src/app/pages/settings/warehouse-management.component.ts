import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEdit } from '@fortawesome/free-solid-svg-icons';
import { WarehouseService } from '../../services/warehouse.service';
import { AuthService } from '../../services/auth.service';
import { Warehouse } from '../../models/warehouse.model';

@Component({
  selector: 'app-warehouse-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    FontAwesomeModule
  ],
  templateUrl: './warehouse-management.component.html',
  styleUrl: './warehouse-management.component.css'
})
export class WarehouseManagementComponent implements OnInit, OnDestroy {
  warehouseForm!: FormGroup;
  warehouses: Warehouse[] = [];
  
  isLoading = false;
  isSubmitting = false;
  showModal = false;
  editingWarehouseId: number | null = null;
  
  // Font Awesome icons
  faEdit = faEdit;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private warehouseService: WarehouseService,
    private authService: AuthService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadWarehouses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize warehouse form
   */
  private initForm(): void {
    this.warehouseForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      status: ['ACTIVE', Validators.required]
    });
  }

  /**
   * Load all warehouses
   */
  private loadWarehouses(): void {
    this.isLoading = true;

    const subscription = this.warehouseService.getAllWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: Warehouse[]) => {
          // Run inside Angular zone to ensure change detection
          this.ngZone.run(() => {
            if (response && Array.isArray(response)) {
              this.warehouses = response;
            } else {
              this.warehouses = [];
            }

            // Set loading to false first
            this.isLoading = false;

            // Then manually trigger change detection to update the view
            this.cdr.detectChanges();
          });
        },
        (error: unknown) => {
          const err = error as { error?: { message?: string } };
          this.toastr.error(err?.error?.message || 'Error loading warehouses');
          this.isLoading = false;
        }
      );
    
    // Safety timeout - set loading to false after 10 seconds
    setTimeout(() => {
      if (this.isLoading) {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    }, 10000);
  }

  /**
   * Submit warehouse form (Create or Update)
   */
  onSubmit(): void {
    if (this.warehouseForm.invalid) {
      this.toastr.error('Please fill all required fields correctly');
      return;
    }

    this.isSubmitting = true;

    if (this.editingWarehouseId) {
      // Update existing warehouse
      const warehouseData = {
        ...this.warehouseForm.value
      };
      
      this.warehouseService.updateWarehouse(this.editingWarehouseId, warehouseData)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          () => {
            this.toastr.success('Warehouse updated successfully');
            this.warehouseService.invalidateCache();
            this.closeModal();
            this.loadWarehouses();
            this.isSubmitting = false;
          },
          (error: unknown) => {
            const err = error as { error?: { message?: string } };
            if (err.error && err.error.message) {
              this.toastr.error(err.error.message);
            } else {
              this.toastr.error('Error updating warehouse');
            }
            this.isSubmitting = false;
          }
        );
    } else {
      // Create new warehouse
      const userInfo = this.authService.getUserInfo();
      const businessId = userInfo?.businessId;
      
      if (!businessId) {
        this.toastr.error('User business information not found');
        this.isSubmitting = false;
        return;
      }

      this.warehouseService.createWarehouse(this.warehouseForm.value.name, businessId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          () => {
            this.toastr.success('Warehouse created successfully');
            this.warehouseService.invalidateCache();
            this.closeModal();
            this.loadWarehouses();
            this.isSubmitting = false;
          },
          (error: unknown) => {
            const err = error as { error?: { message?: string } };
            if (err.error && err.error.message) {
              this.toastr.error(err.error.message);
            } else {
              this.toastr.error('Error creating warehouse');
            }
            this.isSubmitting = false;
          }
        );
    }
  }

  /**
   * Edit warehouse
   */
  editWarehouse(warehouse: Warehouse): void {
    this.editingWarehouseId = warehouse.id;
    this.warehouseForm.patchValue({
      name: warehouse.name,
      status: warehouse.status
    });
    this.showModal = true;
  }

  /**
   * Open form modal to add new warehouse
   */
  onAddNew(): void {
    this.editingWarehouseId = null;
    this.warehouseForm.reset({ status: 'ACTIVE' });
    this.showModal = true;
  }

  /**
   * Close modal
   */
  closeModal(): void {
    this.showModal = false;
    this.editingWarehouseId = null;
    this.resetForm();
  }

  /**
   * Reset form
   */
  resetForm(): void {
    this.warehouseForm.reset({ status: 'ACTIVE' });
    this.editingWarehouseId = null;
  }

  /**
   * Get status badge color
   */
  getStatusColor(status: string): string {
    return status === 'ACTIVE' ? 'badge-active' : 'badge-inactive';
  }

  /**
   * Get status display text
   */
  getStatusText(status: string): string {
    return status === 'ACTIVE' ? 'Active' : 'Inactive';
  }

  /**
   * Check if warehouse is default (cannot delete)
   */
  isDefaultWarehouse(warehouseId: number): boolean {
    return warehouseId === 1 || warehouseId === 2; // Panvel or Karjat
  }

  /**
   * Format date
   */
  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    // Parse and format date in IST format (YYYY-MM-DD)
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${day}/${month}/${year}`;
  }
}
