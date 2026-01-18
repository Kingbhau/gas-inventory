import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WarehouseService } from '../../services/warehouse.service';
import { WarehouseTransferService } from '../../services/warehouse-transfer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { Warehouse } from '../../models/warehouse.model';
import { WarehouseTransfer } from '../../models/warehouse-transfer.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';

@Component({
  selector: 'app-warehouse-transfer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './warehouse-transfer.component.html',
  styleUrl: './warehouse-transfer.component.css'
})
export class WarehouseTransferComponent implements OnInit, OnDestroy {
  transferForm!: FormGroup;
  warehouses: Warehouse[] = [];
  variants: CylinderVariant[] = [];
  transfers: WarehouseTransfer[] = [];
  
  isLoading = false;
  isTransferring = false;
  displayedColumns: string[] = ['referenceNumber', 'fromWarehouse', 'toWarehouse', 'variant', 'quantity', 'transferDate', 'notes'];
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private warehouseService: WarehouseService,
    private warehouseTransferService: WarehouseTransferService,
    private variantService: CylinderVariantService,
    private toastr: ToastrService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadVariants();
    this.loadTransfers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize transfer form with validation
   */
  private initForm(): void {
    this.transferForm = this.fb.group({
      fromWarehouseId: [null, Validators.required],
      toWarehouseId: [null, Validators.required],
      variantId: [null, Validators.required],
      quantity: [null, [Validators.required, Validators.min(1), Validators.max(10000)]],
      notes: ['']
    });
  }

  /**
   * Load active warehouses
   */
  private loadWarehouses(): void {
    this.isLoading = true;
    this.warehouseService.getActiveWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          if (response.success) {
            this.warehouses = response.data;
          } else {
            this.toastr.error('Failed to load warehouses');
          }
          this.isLoading = false;
        },
        (error: any) => {
          this.toastr.error('Error loading warehouses');
          this.isLoading = false;
        }
      );
  }

  /**
   * Load cylinder variants
   */
  private loadVariants(): void {
    this.variantService.getAllVariants()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          if (response.success) {
            this.variants = response.data;
          }
        },
        (error: any) => {
          this.toastr.error('Error loading variants');
        }
      );
  }

  /**
   * Load transfer history
   */
  private loadTransfers(): void {
    this.warehouseTransferService.getAllTransfers()
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          if (response.success) {
            this.transfers = response.data;
          }
        },
        (error: any) => {
          this.toastr.error('Error loading transfer history');
        }
      );
  }

  /**
   * Submit warehouse transfer
   * VALIDATIONS:
   * - Different warehouses
   * - Sufficient stock in source
   * - Positive quantity
   * - Active warehouses
   */
  onSubmit(): void {
    if (this.transferForm.invalid) {
      this.toastr.error('Please fill all required fields correctly');
      return;
    }

    // Validation: Different warehouses
    if (this.transferForm.value.fromWarehouseId === this.transferForm.value.toWarehouseId) {
      this.toastr.error('Source and destination warehouses must be different');
      return;
    }

    this.isTransferring = true;
    const transfer: WarehouseTransfer = this.transferForm.value;

    this.warehouseTransferService.transferCylinders(transfer)
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (response: any) => {
          if (response.success) {
            this.toastr.success('Warehouse transfer completed successfully');
            this.resetForm();
            this.loadTransfers(); // Refresh transfer history
          } else {
            this.toastr.error(response.message || 'Transfer failed');
          }
          this.isTransferring = false;
        },
        (error: any) => {
          if (error.error && error.error.message) {
            this.toastr.error(error.error.message);
          } else {
            this.toastr.error('Error completing transfer');
          }
          this.isTransferring = false;
        }
      );
  }

  /**
   * Reset form
   */
  resetForm(): void {
    this.transferForm.reset();
  }

  /**
   * Get warehouse name by ID
   */
  getWarehouseName(id: number): string {
    const warehouse = this.warehouses.find(w => w.id === id);
    return warehouse ? warehouse.name : 'Unknown';
  }

  /**
   * Get variant name by ID
   */
  getVariantName(id: number): string {
    const variant = this.variants.find(v => v.id === id);
    return variant ? variant.name : 'Unknown';
  }
}
