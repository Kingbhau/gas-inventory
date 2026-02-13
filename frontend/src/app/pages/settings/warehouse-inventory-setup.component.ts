import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSave, faSync } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { WarehouseService } from '../../services/warehouse.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { Warehouse } from '../../models/warehouse.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { InventoryStock } from '../../models/inventory-stock.model';
import { WarehouseInventorySetupRequest } from '../../models/warehouse-inventory.model';
import { SimpleStatusDTO } from '../../models/simple-status';

interface VariantRow {
  variantId: number;
  variantName: string;
  filledQty: number;
  emptyQty: number;
}

@Component({
  selector: 'app-warehouse-inventory-setup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FontAwesomeModule],
  templateUrl: './warehouse-inventory-setup.component.html',
  styleUrl: './warehouse-inventory-setup.component.css'
})
export class WarehouseInventorySetupComponent implements OnInit, OnDestroy {
  faSave = faSave;
  faSync = faSync;

  warehouses: Warehouse[] = [];
  variants: CylinderVariant[] = [];
  selectedWarehouseId: number | null = null;
  inventoryRows: VariantRow[] = [];

  isLoading = false;
  isSaving = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private warehouseService: WarehouseService,
    private variantService: CylinderVariantService,
    private inventoryStockService: InventoryStockService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadVariants();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadWarehouses(): void {
    this.warehouseService.getActiveWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: Warehouse[]) => {
          this.warehouses = data || [];
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.toastr.error('Failed to load warehouses');
        }
      });
  }

  private loadVariants(): void {
    this.variantService.getAllVariantsWithCache()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CylinderVariant[]) => {
          this.variants = Array.isArray(response) ? response : [];
          if (this.selectedWarehouseId) {
            this.loadInventoryForWarehouse(this.selectedWarehouseId);
          }
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.toastr.error('Failed to load variants');
        }
      });
  }

  onWarehouseChange(warehouseId: string | number): void {
    const id = typeof warehouseId === 'string' ? parseInt(warehouseId, 10) : warehouseId;
    this.selectedWarehouseId = id > 0 ? id : null;
    if (this.selectedWarehouseId) {
      this.loadInventoryForWarehouse(this.selectedWarehouseId);
    }
  }

  private loadInventoryForWarehouse(warehouseId: number): void {
    this.isLoading = true;
    this.inventoryStockService.getInventoryByWarehouse(warehouseId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: InventoryStock[]) => {
          const existingInventory = response || [];
          
          // Create rows for all variants, populate with existing data if available
          this.inventoryRows = this.variants
            .filter((variant): variant is CylinderVariant & { id: number } => typeof variant.id === 'number')
            .map((variant) => {
            const existing = existingInventory.find((inv) => inv.variantId === variant.id);
            return {
              variantId: variant.id,
              variantName: variant.name,
              filledQty: existing?.filledQty || 0,
              emptyQty: existing?.emptyQty || 0
            };
          });
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.toastr.error('Failed to load inventory');
          this.isLoading = false;
        }
      });
  }

  updateFilledQty(index: number, value: string): void {
    const qty = parseInt(value) || 0;
    if (qty >= 0) {
      this.inventoryRows[index].filledQty = qty;
    }
  }

  updateEmptyQty(index: number, value: string): void {
    const qty = parseInt(value) || 0;
    if (qty >= 0) {
      this.inventoryRows[index].emptyQty = qty;
    }
  }

  saveInventory(): void {
    if (!this.selectedWarehouseId) {
      this.toastr.error('Please select a warehouse');
      return;
    }

    this.isSaving = true;

    const payload: WarehouseInventorySetupRequest = {
      warehouseId: this.selectedWarehouseId,
      inventoryItems: this.inventoryRows.map((row) => ({
        variantId: row.variantId,
        filledQty: row.filledQty,
        emptyQty: row.emptyQty
      }))
    };

    this.inventoryStockService.setupWarehouseInventory(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (_response: SimpleStatusDTO) => {
          this.toastr.success('Inventory setup saved successfully');
          this.selectedWarehouseId = null;
          this.inventoryRows = [];
          this.isSaving = false;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          const errorObj = err as { error?: { message?: string } };
          this.toastr.error(errorObj?.error?.message || 'Failed to save inventory setup');
          this.isSaving = false;
        }
      });
  }

  resetInventory(): void {
    if (this.selectedWarehouseId) {
      this.loadInventoryForWarehouse(this.selectedWarehouseId);
    }
  }
}
