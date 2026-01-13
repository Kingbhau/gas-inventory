import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

@Component({
  selector: 'app-inventory-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './inventory-management.component.html',
  styleUrl: './inventory-management.component.css'
})
export class InventoryManagementComponent implements OnInit {
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

  // Font Awesome Icons
  faBox = faBox;
  faArrowUp = faArrowUp;
  faArrowDown = faArrowDown;

  stockSummary: any[] = [];
  movements: any[] = [];
  variants: CylinderVariant[] = [];

  constructor(
    private inventoryService: InventoryStockService,
    private ledgerService: CustomerCylinderLedgerService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService
  ) {}

  ngOnInit() {
    this.loadInventory();
    this.loadMovements();
    this.loadVariants();
  }

  loadVariants() {
    this.variantService.getActiveVariants().subscribe({
      next: (data) => this.variants = data,
      error: (err) => {
        this.toastr.error('Failed to load variants', 'Error');
        this.variants = [];
      }
    });
  }

  loadMovements() {
    this.loadingService.show('Loading stock movements...');
    const sub = this.ledgerService.getAllMovements()
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
            reference: entry.refId
          }))
          .sort((a: any, b: any) => (b.id && a.id ? b.id - a.id : new Date(b.date).getTime() - new Date(a.date).getTime()));
        // Reset to page 1 when data is reloaded
        this.movementPage = 1;
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
        this.stockSummary = data?.content || data || [];
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
}
