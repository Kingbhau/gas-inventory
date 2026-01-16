import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBox, faArrowUp, faArrowDown } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { LoadingService } from '../../services/loading.service';
import { of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { SharedModule } from 'src/app/shared/shared.module';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { WarehouseService } from '../../services/warehouse.service';

@Component({
  selector: 'app-inventory-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './inventory-management.component.html',
  styleUrl: './inventory-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InventoryManagementComponent implements OnInit, OnDestroy {
      // Pagination state for stock movement table
      movementPage = 1;
      filterType = '';
      movementPageSize = 10;
    // Assign a color to each variant name (simple hash for demo)
    getVariantColor(variantName: string): string {
      if (!variantName) return '#888';
      const colors = ['#1976d2', '#388e3c', '#fbc02d', '#d32f2f', '#7b1fa2', '#0288d1', '#c2185b'];
      let hash = 0;
      for (let i = 0; i < variantName.length; i++) {
        hash = variantName.charCodeAt(i) + ((hash << 5) - hash);
      }
      return colors[Math.abs(hash) % colors.length];
    }
  activeTab = 'summary';
  filterVariant = '';
  selectedWarehouse: any = null;
  warehouses: any[] = [];

  // Font Awesome Icons
  faBox = faBox;
  faArrowUp = faArrowUp;
  faArrowDown = faArrowDown;

  stockSummary: any[] = [];
  totalStockSummary: any[] = []; // Total across all warehouses
  movements: any[] = [];
  variants: CylinderVariant[] = [];

  // Stock Transfer Modal
  showTransferModal = false;
  stockTransferForm!: FormGroup;
  availableStock: any = null; // Track available stock for selected variant/warehouse

  constructor(
    private inventoryService: InventoryStockService,
    private ledgerService: CustomerCylinderLedgerService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService,
    private warehouseService: WarehouseService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder
  ) {
    this.initStockTransferForm();
  }

  ngOnInit() {
    this.loadWarehouses();
    this.loadInventory();
    this.loadMovements();
    this.loadVariants();
  }

  loadWarehouses() {
    this.warehouseService.getActiveWarehouses().subscribe({
      next: (response: any) => {
        this.warehouses = (response && response.data) || [];
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.toastr.error('Failed to load warehouses', 'Error');
        this.warehouses = [];
        this.cdr.markForCheck();
      }
    });
  }

  onWarehouseChange() {
    // Reset to page 1 and reload inventory when warehouse changes
    this.movementPage = 1;
    this.loadInventory();
    this.loadMovements();
  }

  ngOnDestroy() {
    // Component destroyed - data will be reloaded on next visit
  }

  loadVariants() {
    this.variantService.getActiveVariants().subscribe({
      next: (data) => {
        this.variants = data;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toastr.error('Failed to load variants', 'Error');
        this.variants = [];
        this.cdr.markForCheck();
      }
    });
  }

  loadMovements() {
    this.loadingService.show('Loading stock movements...');
    
    // Use warehouse-specific endpoint if a warehouse is selected
    const movementSource = this.selectedWarehouse?.id 
      ? this.ledgerService.getMovementsByWarehouse(this.selectedWarehouse.id)
      : this.ledgerService.getAllMovements();
    
    const sub = movementSource
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading stock movements';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of([]);
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: any[]) => {
        this.movements = (data || [])
          .map((entry: any) => ({
            id: entry.id,
            date: entry.transactionDate,
            customer: entry.customerName,
            variant: entry.variantName,
            type: entry.refType === 'SALE' ? 'Sale' : (entry.refType === 'EMPTY_RETURN' ? 'Return' : entry.refType),
            filledQty: entry.filledOut,
            emptyQty: entry.emptyIn,
            balance: entry.balance,
            reference: entry.refId,
            fromWarehouse: entry.fromWarehouseName,
            toWarehouse: entry.toWarehouseName
          }))
          .sort((a: any, b: any) => (b.id && a.id ? b.id - a.id : new Date(b.date).getTime() - new Date(a.date).getTime()));
        // Reset to page 1 when data is reloaded
        this.movementPage = 1;
        this.cdr.markForCheck();
        sub.unsubscribe();
      });
  }

  onPageChange(page: number) {
    this.movementPage = page;
    // Scroll to top of table when page changes
    const tableElement = document.querySelector('.table-wrapper');
    if (tableElement) {
      tableElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  onPageSizeChange(size: number) {
    this.movementPageSize = size;
    this.movementPage = 1;
  }

  onFilterChange() {
    // Reset to page 1 when filter changes
    this.movementPage = 1;
  }

  loadInventory() {
    // Load total inventory (all warehouses)
    const sub = this.inventoryService.getAllStock(0, 100)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading inventory';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of([]);
        })
      )
      .subscribe((data: any) => {
        const allStockData = data?.content || data || [];
        
        // If a warehouse is selected, load warehouse-specific inventory
        if (this.selectedWarehouse?.id) {
          this.inventoryService.getStockByWarehouse(this.selectedWarehouse.id).subscribe({
            next: (warehouseData: any) => {
              this.stockSummary = warehouseData?.content || warehouseData || [];
              this.cdr.markForCheck();
            },
            error: (error) => {
              this.toastr.error('Error loading warehouse inventory', 'Error');
              this.stockSummary = [];
              this.cdr.markForCheck();
            }
          });
        } else {
          // Aggregate stock by variant when showing all warehouses
          const aggregatedStock = new Map<string, any>();
          
          allStockData.forEach((stock: any) => {
            const variantName = stock.variantName || 'Unknown';
            if (!aggregatedStock.has(variantName)) {
              aggregatedStock.set(variantName, {
                variantName: variantName,
                filledQty: 0,
                emptyQty: 0
              });
            }
            const existing = aggregatedStock.get(variantName);
            existing.filledQty += stock.filledQty || 0;
            existing.emptyQty += stock.emptyQty || 0;
          });
          
          this.stockSummary = Array.from(aggregatedStock.values());
        }
        this.cdr.markForCheck();
        sub.unsubscribe();
      });
  }

  get stock19kg() {
    return this.stockSummary[0];
  }

  get stock5kg() {
    return this.stockSummary[1];
  }

  get filteredMovements() {
      let filtered = this.movements;
      if (this.filterVariant) {
        filtered = filtered.filter(m => m.variant === this.filterVariant);
      }
      if (this.filterType) {
        filtered = filtered.filter(m => m.type === this.filterType);
      }
      return filtered;
  }

  get paginatedMovements() {
    const filtered = this.filteredMovements;
    const startIndex = (this.movementPage - 1) * this.movementPageSize;
    const endIndex = startIndex + this.movementPageSize;
    return filtered.slice(startIndex, endIndex);
  }

  get totalMovements() {
    return this.filteredMovements.length;
  }

  get totalPages() {
    return Math.ceil(this.totalMovements / this.movementPageSize) || 1;
  }

  getTotalPages() {
    return this.totalPages;
  }

  getVariantVariety() {
    return [...new Set(this.movements.map(m => m.variant))];
  }

  getMovementColor(type: string): string {
    return type === 'filled' ? '#16a34a' : '#991b1b';
  }

  getStockHeaderTitle(): string {
    if (this.selectedWarehouse?.id) {
      return `Stock - ${this.selectedWarehouse.name} Warehouse`;
    }
    return 'Stock - Total (All Warehouses)';
  }

  initStockTransferForm(): void {
    this.stockTransferForm = this.fb.group({
      fromWarehouseId: [null, Validators.required],
      toWarehouseId: [null, Validators.required],
      variantId: [null, Validators.required],
      filledQty: [null, [Validators.required, Validators.min(0)]],
      emptyQty: [null, Validators.min(0)]
    });
  }

  openStockTransferModal(): void {
    this.showTransferModal = true;
    this.initStockTransferForm();
    this.setupFormValueChangeListeners();
  }

  setupFormValueChangeListeners(): void {
    // Listen for changes in fromWarehouseId and variantId to load available stock
    this.stockTransferForm.get('fromWarehouseId')?.valueChanges.subscribe(() => {
      this.updateAvailableStock();
    });
    this.stockTransferForm.get('variantId')?.valueChanges.subscribe(() => {
      this.updateAvailableStock();
    });
  }

  updateAvailableStock(): void {
    const fromWarehouseId = this.stockTransferForm.get('fromWarehouseId')?.value;
    const variantId = this.stockTransferForm.get('variantId')?.value;

    if (fromWarehouseId && variantId) {
      // Find the stock info from our loaded inventory
      const warehouse = this.warehouses.find(w => w.id === fromWarehouseId);
      if (warehouse && this.selectedWarehouse?.id === fromWarehouseId) {
        // If same warehouse is selected, use cached summary
        const stock = this.stockSummary.find(s => s.variantId === variantId);
        if (stock) {
          this.availableStock = {
            filledQty: stock.filledQty || 0,
            emptyQty: stock.emptyQty || 0,
            variantName: stock.variantName
          };
        }
      } else if (warehouse) {
        // Otherwise load for this specific warehouse
        this.inventoryService.getStockByWarehouse(fromWarehouseId).subscribe({
          next: (stocks) => {
            const stock = stocks.find((s: any) => s.variantId === variantId);
            if (stock) {
              this.availableStock = {
                filledQty: stock.filledQty || 0,
                emptyQty: stock.emptyQty || 0,
                variantName: stock.variantName
              };
            } else {
              this.availableStock = null;
            }
          },
          error: () => {
            this.availableStock = null;
          }
        });
      }
    } else {
      this.availableStock = null;
    }
  }

  closeStockTransferModal(): void {
    this.showTransferModal = false;
    this.stockTransferForm.reset();
  }

  submitStockTransfer(): void {
    if (this.stockTransferForm.invalid) {
      this.toastr.error('Please fill all required fields', 'Validation Error');
      Object.keys(this.stockTransferForm.controls).forEach(key => {
        this.stockTransferForm.get(key)?.markAsTouched();
      });
      return;
    }

    const formValue = this.stockTransferForm.value;
    
    // Validate that warehouses are different
    if (formValue.fromWarehouseId === formValue.toWarehouseId) {
      this.toastr.error('Source and destination warehouses must be different', 'Invalid Selection');
      return;
    }

    // Validate that at least one quantity is being transferred
    if (formValue.filledQty === 0 && formValue.emptyQty === 0) {
      this.toastr.error('Please enter at least one cylinder quantity to transfer', 'Empty Transfer');
      return;
    }

    // Validate sufficient stock
    if (this.availableStock) {
      if (formValue.filledQty > this.availableStock.filledQty) {
        this.toastr.error(
          `Insufficient filled cylinders. You have ${this.availableStock.filledQty} but trying to transfer ${formValue.filledQty}`,
          'Insufficient Stock'
        );
        return;
      }
      if (formValue.emptyQty > this.availableStock.emptyQty) {
        this.toastr.error(
          `Insufficient empty cylinders. You have ${this.availableStock.emptyQty} but trying to transfer ${formValue.emptyQty}`,
          'Insufficient Stock'
        );
        return;
      }
    }

    const transferRequest = {
      fromWarehouseId: formValue.fromWarehouseId,
      toWarehouseId: formValue.toWarehouseId,
      variantId: formValue.variantId,
      filledQty: formValue.filledQty || 0,
      emptyQty: formValue.emptyQty || 0
    };

    this.inventoryService.transferStock(transferRequest).subscribe({
      next: (response) => {
        this.toastr.success('Stock transferred successfully', 'Transfer Complete');
        this.closeStockTransferModal();
        this.loadInventory();
        this.loadMovements();
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Failed to transfer stock';
        this.toastr.error(errorMessage, 'Transfer Failed');
      }
    });
  }
}
