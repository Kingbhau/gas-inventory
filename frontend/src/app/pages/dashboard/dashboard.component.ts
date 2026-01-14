import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import { faGauge, faBoxes, faShoppingCart, faExclamationTriangle, faEllipsis } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { SaleService } from '../../services/sale.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs/operators';

interface KPICard {
  icon: IconDefinition;
  label: string;
  value: number | string;
  subtext: string;
  color: 'blue' | 'green' | 'orange' | 'red';
}

interface SalesSummaryRow {
  variant: string;
  filled: number;
  empty: number;
}

interface PendingCylindersRow {
  customer: string;
  variant: string;
  pending: number;
}

interface RecentSaleRow {
  customer: string;
  date: string;
  amount: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, SharedModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit, OnDestroy {
  todayDate = new Date();

  // Font Awesome Icons
  faGauge = faGauge;
  faBoxes = faBoxes;
  faShoppingCart = faShoppingCart;
  faExclamationTriangle = faExclamationTriangle;
  faEllipsis = faEllipsis;

  kpiCards: KPICard[] = [
    { icon: faShoppingCart, label: 'Total Sales', value: 0, subtext: '₹0', color: 'green' },
    { icon: faBoxes, label: 'Total Inventory', value: 0, subtext: '0 filled, 0 empty', color: 'blue' },
    { icon: faExclamationTriangle, label: 'Pending Collections', value: 0, subtext: 'empty cylinders', color: 'orange' },
    { icon: faGauge, label: 'Avg Sale Value', value: 0, subtext: '₹0', color: 'red' }
  ];
  inventorySummary: SalesSummaryRow[] = [];
  inventoryPage = 1;
  inventoryPageSize = 10;
  inventoryTotalPages = 1;
  inventoryTotalElements = 0;
  inventoryPageSizeOptions = [5, 10, 20, 50, 100];

  pendingCylinders: PendingCylindersRow[] = [];

  recentSales: RecentSaleRow[] = [];
  // Only show last 5 recent sales

  constructor(
    private inventoryService: InventoryStockService,
    private saleService: SaleService,
    private ledgerService: CustomerCylinderLedgerService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadDashboardData();
  }

  ngOnDestroy() {
    // Component destroyed - data will be reloaded on next visit
  }

  loadDashboardData() {
    this.loadingService.show('Loading dashboard...');
    // Load sales summary with all totals calculated on backend
    this.saleService.getSalesSummary().pipe(
      finalize(() => this.loadingService.hide())
    ).subscribe({
      next: (data) => {
        this.kpiCards[0] = { ...this.kpiCards[0], value: data.transactionCount, subtext: `₹${data.totalSalesAmount.toFixed(0)}` };
        this.kpiCards[3] = { ...this.kpiCards[3], value: Math.round(data.avgSaleValue), subtext: `₹${data.avgSaleValue.toFixed(0)}` };
        this.cdr.markForCheck();
      },
      error: (error) => {
        const errorMessage = error?.error?.message || error?.message || 'Error loading sales';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
      }
    });

    this.loadInventorySummary();
    this.loadRecentSales();

    // Load return pending data using pending-summary endpoint
    this.ledgerService.getAllReturnPendingSummary().subscribe({
      next: (data: any) => {
        this.pendingCylinders = data
          .sort((a: any, b: any) => (b.id || 0) - (a.id || 0))
          .map((ledger: any) => ({
            customer: ledger.customerName || 'Unknown',
            variant: ledger.variantName || 'Unknown',
            pending: ledger.balance || 0
          }))
          .filter((row: any) => row.pending > 0);

        const totalReturnPending = data.reduce((sum: number, ledger: any) => sum + (ledger.balance || 0), 0);

        // Update KPI cards with return pending data
        this.kpiCards[2] = { ...this.kpiCards[2], value: totalReturnPending, subtext: 'cylinders pending return' };
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        const errorMessage = error?.error?.message || error?.message || 'Error loading return pending summary';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
      }
    });
  }

  loadInventorySummary() {
    this.inventoryService.getAllStock(this.inventoryPage - 1, this.inventoryPageSize).subscribe({
      next: (data) => {
        const pageRows = data.content || [];
        this.inventorySummary = pageRows.map((stock: any) => ({
          variant: stock.variantName || 'Unknown',
          filled: stock.filledQty || 0,
          empty: stock.emptyQty || 0
        }));
        this.inventoryTotalElements = data.totalElements || pageRows.length;
        this.inventoryTotalPages = data.totalPages || 1;

        const totalFilled = pageRows.reduce((sum: number, stock: any) => sum + (stock.filledQty || 0), 0);
        const totalEmpty = pageRows.reduce((sum: number, stock: any) => sum + (stock.emptyQty || 0), 0);
        this.kpiCards[1] = { ...this.kpiCards[1], value: totalFilled + totalEmpty, subtext: `${totalFilled} filled, ${totalEmpty} empty` };
        this.cdr.markForCheck();
      },
      error: (error) => {
        const errorMessage = error?.error?.message || error?.message || 'Error loading inventory';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
      }
    });
  }

  loadRecentSales() {
    this.saleService.getRecentSales().subscribe({
      next: (salesArray) => {
        this.recentSales = salesArray.map((sale: any) => ({
          customer: sale.customerName || 'Unknown',
          date: new Date(sale.saleDate).toLocaleDateString(),
          amount: sale.totalAmount?.toString() || '0'
        }));
        this.cdr.markForCheck();
      },
      error: (error) => {
        const errorMessage = error?.error?.message || error?.message || 'Error loading recent sales';
        this.toastr.error(errorMessage, 'Error');
        console.error('Full error:', error);
      }
    });
  }

  onInventoryPageChange(page: number) {
    this.inventoryPage = page;
    this.loadInventorySummary();
  }

  onInventoryPageSizeChange(size: number) {
    this.inventoryPageSize = size;
    this.inventoryPage = 1;
    this.loadInventorySummary();
  }


}
