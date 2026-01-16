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
  variants: any[] = [];
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
    this.warehouseService.getAllWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.warehouses = response.data || response || [];
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.toastr.error('Failed to load warehouses');
          console.error(err);
        }
      });
  }

  private loadVariants(): void {
    this.variantService.getAllVariants(0, 1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.variants = response.content || response.data || [];
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.toastr.error('Failed to load variants');
          console.error(err);
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
        next: (response: any) => {
          const existingInventory = response.data || response || [];
          
          // Create rows for all variants, populate with existing data if available
          this.inventoryRows = this.variants.map(variant => {
            const existing = existingInventory.find((inv: any) => inv.variantId === variant.id);
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
        error: (err: any) => {
          this.toastr.error('Failed to load inventory');
          console.error(err);
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

    const payload = {
      warehouseId: this.selectedWarehouseId,
      inventoryItems: this.inventoryRows.map(row => ({
        variantId: row.variantId,
        filledQty: row.filledQty,
        emptyQty: row.emptyQty
      }))
    };

    this.inventoryStockService.setupWarehouseInventory(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.toastr.success('Inventory setup saved successfully');
          this.isSaving = false;
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.toastr.error(err.error?.message || 'Failed to save inventory setup');
          console.error(err);
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
