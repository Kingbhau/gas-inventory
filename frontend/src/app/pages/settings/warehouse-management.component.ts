import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPencil, faTrash, faCheck, faBan } from '@fortawesome/free-solid-svg-icons';
import { WarehouseService } from '../../services/warehouse.service';
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
  faPencil = faPencil;
  faTrash = faTrash;
  faCheck = faCheck;
  faBan = faBan;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private warehouseService: WarehouseService,
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
    console.log('🔵 Starting loadWarehouses...');
    
    const subscription = this.warehouseService.getAllWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          console.log('🟢 Received response from API');
          console.log('🔹 Full warehouse response:', response);
          console.log('🔹 Response type:', typeof response);
          console.log('🔹 Response is array?', Array.isArray(response));
          
          // Run inside Angular zone to ensure change detection
          this.ngZone.run(() => {
            if (response && Array.isArray(response)) {
              // Backend returns array directly
              console.log('✅ Backend returned array directly');
              this.warehouses = response;
            } else if (response && response.success && response.data) {
              // Standard wrapper format
              console.log('✅ Standard wrapper format with success=true');
              this.warehouses = Array.isArray(response.data) ? response.data : [response.data];
            } else if (response && response.data) {
              // Has data property but no success flag
              console.log('✅ Data property exists (no success flag)');
              this.warehouses = Array.isArray(response.data) ? response.data : [response.data];
            } else if (response) {
              // Response exists but structure unclear
              console.log('⚠️ Response exists but structure unclear:', Object.keys(response));
              this.warehouses = [];
              this.toastr.error('Failed to parse warehouse response');
            } else {
              console.warn('⚠️ No response received');
              this.warehouses = [];
            }
            
            console.log(`✅ Warehouses loaded: ${this.warehouses.length} items`);
            console.log('📋 Warehouses array:', this.warehouses);
            console.log('📋 Array is still array?:', Array.isArray(this.warehouses));
            
            if (this.warehouses.length > 0) {
              console.log('First warehouse object:', this.warehouses[0]);
              console.log('First warehouse keys:', Object.keys(this.warehouses[0]));
            }
            
            // Set loading to false first
            this.isLoading = false;
            console.log('✅ isLoading set to false');
            
            // Then manually trigger change detection to update the view
            this.cdr.detectChanges();
            console.log('✅ Change detection triggered');
          });
        },
        (error: any) => {
          console.error('❌ Error loading warehouses:', error);
          this.toastr.error(error?.error?.message || 'Error loading warehouses');
          this.isLoading = false;
        }
      );
    
    // Safety timeout - set loading to false after 10 seconds
    setTimeout(() => {
      if (this.isLoading) {
        console.warn('⚠️ Timeout: isLoading still true after 10 seconds');
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
          (response: any) => {
            if (response.success) {
              this.toastr.success('Warehouse updated successfully');
              this.closeModal();
              this.loadWarehouses();
            } else {
              this.toastr.error(response.message || 'Update failed');
            }
            this.isSubmitting = false;
          },
          (error: any) => {
            if (error.error && error.error.message) {
              this.toastr.error(error.error.message);
            } else {
              this.toastr.error('Error updating warehouse');
            }
            this.isSubmitting = false;
          }
        );
    } else {
      // Create new warehouse
      this.warehouseService.createWarehouse(this.warehouseForm.value.name)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          (response: any) => {
            if (response.success) {
              this.toastr.success('Warehouse created successfully');
              this.closeModal();
              this.loadWarehouses();
            } else {
              this.toastr.error(response.message || 'Creation failed');
            }
            this.isSubmitting = false;
          },
          (error: any) => {
            if (error.error && error.error.message) {
              this.toastr.error(error.error.message);
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
   * Activate warehouse
   */
  activateWarehouse(warehouseId: number): void {
    if (confirm('Are you sure you want to activate this warehouse?')) {
      this.warehouseService.activateWarehouse(warehouseId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          (response: any) => {
            if (response.success) {
              this.toastr.success('Warehouse activated successfully');
              this.loadWarehouses();
            } else {
              this.toastr.error(response.message || 'Activation failed');
            }
          },
          (error: any) => {
            this.toastr.error('Error activating warehouse');
          }
        );
    }
  }

  /**
   * Deactivate warehouse
   */
  deactivateWarehouse(warehouseId: number): void {
    if (confirm('Are you sure you want to deactivate this warehouse? Transfers will be blocked.')) {
      this.warehouseService.deactivateWarehouse(warehouseId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(
          (response: any) => {
            if (response.success) {
              this.toastr.success('Warehouse deactivated successfully');
              this.loadWarehouses();
            } else {
              this.toastr.error(response.message || 'Deactivation failed');
            }
          },
          (error: any) => {
            this.toastr.error('Error deactivating warehouse');
          }
        );
    }
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
    return status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE';
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
    return new Date(dateString).toLocaleDateString();
  }
}
