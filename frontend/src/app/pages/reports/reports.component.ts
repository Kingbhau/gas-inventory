import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'variantReturnPendingFilter', standalone: true })
export class VariantReturnPendingFilterPipe implements PipeTransform {
  transform(items: any[], variantName: string): any[] {
    if (!variantName) return items;
    return items.filter(item => item.variant === variantName);
  }
}


import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { Component, OnInit } from '@angular/core';
import { catchError, of, finalize } from 'rxjs';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faShoppingCart, faHourglassEnd, faBox, faTruck, faDownload, faReceipt } from '@fortawesome/free-solid-svg-icons';
import { exportSalesReportToPDF } from './export-sales-report.util';
import { BusinessInfoService } from '../../services/business-info.service';
import { ToastrService } from 'ngx-toastr';
import { SaleService } from '../../services/sale.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { SupplierTransactionService } from '../../services/supplier-transaction.service';
import { LoadingService } from '../../services/loading.service';
import { ExpenseReportComponent } from '../expenses/expense-report.component';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';



interface ReportOption {
  id: string;
  name: string;
  icon: any;
}


@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, VariantReturnPendingFilterPipe, ExpenseReportComponent, AutocompleteInputComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  agencyName: string = '';
        private filtersEverApplied = false;
      // Helper methods for pagination (Angular templates can't use Math)
      // (No longer needed, backend-driven paging)
    // Pagination state for each report table
    salesPage = 1;
    salesPageSize = 10;
    salesTotalPages = 1;
    salesTotalElements = 0;
    returnPendingPage = 1;
    returnPendingPageSize = 10;
    returnPendingTotalPages = 1;
    returnPendingTotalElements = 0;
    inventoryPage = 1;
    inventoryPageSize = 10;
    inventoryTotalPages = 1;
    inventoryTotalElements = 0;
    supplierPage = 1;
    supplierPageSize = 10;
    supplierTotalPages = 1;
    supplierTotalElements = 0;

    // Paginated getters
    // Flattened sale items for row-based pagination
    get flattenedSaleItems() {
      // Each item: { sale, item }
      const filteredSales = (this.salesData || []).filter(sale => sale && sale.totalAmount != null);
      let minAmount = typeof this.filterMinAmount === 'number' && !isNaN(this.filterMinAmount) ? this.filterMinAmount : null;
      let maxAmount = typeof this.filterMaxAmount === 'number' && !isNaN(this.filterMaxAmount) ? this.filterMaxAmount : null;
      let items: any[] = [];
      if (this.filterVariantId) {
        const variantIdNum = Number(this.filterVariantId);
        items = filteredSales.flatMap(sale =>
          (sale.saleItems || [])
            .filter((item: any) => {
              let match = item.variantId === variantIdNum;
              if (minAmount !== null) match = match && item.finalPrice >= minAmount;
              if (maxAmount !== null) match = match && item.finalPrice <= maxAmount;
              return match;
            })
            .map((item: any) => ({ sale, item }))
        );
      } else {
        items = filteredSales.flatMap(sale =>
          (sale.saleItems || [])
            .filter((item: any) => {
              let match = true;
              if (minAmount !== null) match = match && item.finalPrice >= minAmount;
              if (maxAmount !== null) match = match && item.finalPrice <= maxAmount;
              return match;
            })
            .map((item: any) => ({ sale, item }))
        );
      }
      // Sort by sale date descending (latest first)
      return items.sort((a, b) => {
        const dateA = new Date(a.sale.saleDate).getTime();
        const dateB = new Date(b.sale.saleDate).getTime();
        return dateB - dateA;
      });
    }
    get paginatedSalesData() {
      // Already paged from backend
      return this.flattenedSaleItems;
    }
    get paginatedReturnPendingData() {
      return this.returnPendingData;
    }
    get paginatedInventoryData() {
      return this.inventoryData;
    }
    get paginatedSupplierData() {
      return this.supplierData;
    }
  // Sales filters
  filterCustomerId: string = '';
  filterVariantId: string = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;
  filterReturnPendingVariantId: string = '';
  customersList: any[] = [];
  variantsList: any[] = [];
  selectedReport = 'sales';
  filterFromDate = '';
  filterToDate = '';

  // Font Awesome Icons
  faShoppingCart = faShoppingCart;
  faHourglassEnd = faHourglassEnd;
  faBox = faBox;
  faTruck = faTruck;
  faDownload = faDownload;
  faReceipt = faReceipt;

  availableReports: ReportOption[] = [
    { id: 'sales', name: 'Sales Report', icon: faShoppingCart },
    { id: 'expenses', name: 'Expenses Report', icon: faReceipt },
    { id: 'returnPending', name: 'Return Pending', icon: faHourglassEnd },
    { id: 'inventory', name: 'Inventory', icon: faBox },
    { id: 'supplier', name: 'Supplier', icon: faTruck }
  ];

  // Sales Data
  salesData: any[] = [];

  // Return Pending Data
  returnPendingData: any[] = [];

  // Inventory Data
  inventoryData: any[] = [];

  // Supplier Data
  supplierData: any[] = [];

  // Summary card values
  summaryTotalSalesAmount: number = 0;
  summaryTransactionCount: number = 0;
  summaryAvgSaleValue: number = 0;
  summaryTopCustomer: string = 'N/A';

  constructor(
    private saleService: SaleService,
    private ledgerService: CustomerCylinderLedgerService,
    private inventoryService: InventoryStockService,
    private supplierTransactionService: SupplierTransactionService,
    private customerService: CustomerService,
    private variantService: CylinderVariantService,
    private toastr: ToastrService,
    private businessInfoService: BusinessInfoService,
    private loadingService: LoadingService
  ) {}

  ngOnInit() {
    // Fetch agency name from backend
    this.businessInfoService.getBusinessInfoById(1).subscribe({
      next: (data) => {
        this.agencyName = data.agencyName || '';
      },
      error: () => {
        this.agencyName = '';
      }
    });
    if (this.selectedReport === 'sales') {
      this.applyFilters(false);
    } else {
      this.loadReportData();
    }
    this.customerService.getAllCustomers(0, 100).subscribe({
      next: (data) => {
        this.customersList = data.content || data;
      },
      error: () => { this.customersList = []; }
    });
    this.variantService.getAllVariants(0, 100).subscribe({
      next: (data) => {
        this.variantsList = data.content || data;
      },
      error: () => { this.variantsList = []; }
    });
  }

  loadReportData() {
    if (this.selectedReport === 'sales') {
      this.applyFilters(false);
    }
    if (this.selectedReport === 'returnPending') {
      this.loadReturnPendingData();
    }
    if (this.selectedReport === 'inventory') {
      this.loadInventoryData();
    }
    if (this.selectedReport === 'supplier') {
      this.loadSupplierData();
    }
  }

  loadReturnPendingData() {
    // Simulate paging for return pending (if backend supports, update here)
    const sub = this.ledgerService.getAllReturnPendingSummary()
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading return pending data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of([]);
        })
      )
      .subscribe((data: any) => {
        this.returnPendingData = data
          .sort((a: any, b: any) => (b.id || 0) - (a.id || 0))
          .map((ledger: any) => ({
            customer: ledger.customerName || 'Unknown',
            variant: ledger.variantName || 'Unknown',
            returnPending: ledger.balance || 0
          }))
          .slice((this.returnPendingPage - 1) * this.returnPendingPageSize, this.returnPendingPage * this.returnPendingPageSize);
        this.returnPendingTotalElements = data.length;
        this.returnPendingTotalPages = Math.ceil(data.length / this.returnPendingPageSize) || 1;
        sub.unsubscribe();
      });
  }

  loadInventoryData() {
    this.loadingService.show('Loading inventory data...');
    const sub = this.inventoryService.getAllStock(this.inventoryPage - 1, this.inventoryPageSize)
      .pipe(
        finalize(() => this.loadingService.hide()),
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading inventory data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        })
      )
      .subscribe((data: any) => {
        const stockArray = data.content || data;
        this.inventoryData = stockArray.map((stock: any) => ({
          variant: stock.variantName || 'Unknown',
          filled: stock.filledQty || 0,
          empty: stock.emptyQty || 0
        }));
        this.inventoryTotalElements = data.totalElements || this.inventoryData.length;
        this.inventoryTotalPages = data.totalPages || 1;
        sub.unsubscribe();
      });
  }

  loadSupplierData() {
    this.loadingService.show('Loading supplier data...');
    const sub = this.supplierTransactionService.getAllTransactions(this.supplierPage - 1, this.supplierPageSize)
      .pipe(
        finalize(() => this.loadingService.hide()),
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading supplier data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        })
      )
      .subscribe((data: any) => {
        const transactionArray = data.content || data;
        const supplierMap = new Map();
        transactionArray.forEach((transaction: any) => {
          if (!supplierMap.has(transaction.supplierId)) {
            supplierMap.set(transaction.supplierId, { filled: 0, empty: 0, name: transaction.supplierName });
          }
          const supplier = supplierMap.get(transaction.supplierId);
          supplier.filled += transaction.filledReceived || 0;
          supplier.empty += transaction.emptySent || 0;
        });
        this.supplierData = Array.from(supplierMap.values());
        this.supplierTotalElements = data.totalElements || this.supplierData.length;
        this.supplierTotalPages = data.totalPages || 1;
        sub.unsubscribe();
      });
  }
  // Pagination handlers
  onSalesPageChange(page: number) {
    this.salesPage = page;
    this.applyFilters();
  }
  onSalesPageSizeChange(size: number) {
    this.salesPageSize = size;
    this.salesPage = 1;
    this.applyFilters();
  }
  onReturnPendingPageChange(page: number) {
    this.returnPendingPage = page;
    this.loadReturnPendingData();
  }
  onReturnPendingPageSizeChange(size: number) {
    this.returnPendingPageSize = size;
    this.returnPendingPage = 1;
    this.loadReturnPendingData();
  }
  onInventoryPageChange(page: number) {
    this.inventoryPage = page;
    this.loadInventoryData();
  }
  onInventoryPageSizeChange(size: number) {
    this.inventoryPageSize = size;
    this.inventoryPage = 1;
    this.loadInventoryData();
  }
  onSupplierPageChange(page: number) {
    this.supplierPage = page;
    this.loadSupplierData();
  }
  onSupplierPageSizeChange(size: number) {
    this.supplierPageSize = size;
    this.supplierPage = 1;
    this.loadSupplierData();
  }

  get totalSalesAmount(): number {
    return this.summaryTotalSalesAmount;
  }
  get avgSaleValue(): number {
    return this.summaryAvgSaleValue;
  }
  get topCustomer(): string {
    return this.summaryTopCustomer;
  }
  get transactionCount(): number {
    return this.summaryTransactionCount;
  }
  get filteredReturnPendingData(): any[] {
    if (!Array.isArray(this.returnPendingData)) return [];
    if (!this.filterReturnPendingVariantId) return this.returnPendingData;
    return this.returnPendingData.filter(item => item.variant === this.filterReturnPendingVariantId);
  }

  get totalReturnPending(): number {
    return this.filteredReturnPendingData.reduce((sum, item) => {
      const pending = Number(item.returnPending) || 0;
      return sum + (pending > 0 ? pending : 0);
    }, 0);
  }

  get customersWithReturnPending(): number {
    return this.filteredReturnPendingData.filter(item => (Number(item.returnPending) || 0) > 0).length;
  }

  get highRiskCount(): number {
    return this.filteredReturnPendingData.filter(item => (Number(item.returnPending) || 0) > 10).length;
  }

  getHighRiskReturnPendingCount(): number {
    return this.filteredReturnPendingData.filter(item => item.returnPending > 10).length;
  }

  get totalSupplierFilled(): number {
    if (!Array.isArray(this.supplierData)) return 0;
    return this.supplierData.reduce((sum, supplier) => {
      const filled = Number(supplier.filled) || 0;
      return sum + (filled > 0 ? filled : 0);
    }, 0);
  }

  get totalSupplierEmpty(): number {
    if (!Array.isArray(this.supplierData)) return 0;
    return this.supplierData.reduce((sum, supplier) => {
      const empty = Number(supplier.empty) || 0;
      return sum + (empty > 0 ? empty : 0);
    }, 0);
  }


  async applyFilters(showToastr: boolean = true) {
    // Use current page and size
    const targetPage = this.salesPage;
    const targetSize = this.salesPageSize;
    const fromDate = this.filterFromDate ? this.filterFromDate : undefined;
    const toDate = this.filterToDate ? this.filterToDate : undefined;
    const customerId = this.filterCustomerId ? this.filterCustomerId : undefined;
    const variantId = this.filterVariantId ? Number(this.filterVariantId) : undefined;
    const minAmount = (typeof this.filterMinAmount === 'number' && !isNaN(this.filterMinAmount)) ? this.filterMinAmount : undefined;
    const maxAmount = (typeof this.filterMaxAmount === 'number' && !isNaN(this.filterMaxAmount)) ? this.filterMaxAmount : undefined;
    const sub = this.saleService.getAllSales(targetPage - 1, targetSize, 'id', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading sales data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        })
      )
      .subscribe((data: any) => {
        this.salesData = data.content || data;
        this.salesTotalElements = data.totalElements || this.salesData.length;
        this.salesTotalPages = data.totalPages || 1;
        if (showToastr) {
          this.toastr.info('Filters applied', 'Info');
        }
        sub.unsubscribe();
      });
    // Fetch summary card values from backend summary endpoint
    this.saleService.getSalesSummary(fromDate, toDate, customerId, variantId, minAmount, maxAmount)
      .pipe(
        catchError((error: any) => {
          this.summaryTotalSalesAmount = 0;
          this.summaryTransactionCount = 0;
          this.summaryAvgSaleValue = 0;
          this.summaryTopCustomer = 'N/A';
          return of({});
        })
      )
      .subscribe((summary: any) => {
        this.summaryTotalSalesAmount = summary.totalSalesAmount || 0;
        this.summaryTransactionCount = summary.transactionCount || 0;
        this.summaryAvgSaleValue = summary.avgSaleValue || 0;
        this.summaryTopCustomer = summary.topCustomer || 'N/A';
      });
  }

  // Ensure Generate button resets to page 1 and applies filters
  onGenerateClick() {
    this.salesPage = 1;
    this.filtersEverApplied = true;
    this.applyFilters(true);
  }

  exportReport() {
    if (this.selectedReport === 'sales') {
      const fromDate = this.filterFromDate ? this.filterFromDate : undefined;
      const toDate = this.filterToDate ? this.filterToDate : undefined;
      const customerId = this.filterCustomerId ? this.filterCustomerId : undefined;
      const variantId = this.filterVariantId ? Number(this.filterVariantId) : undefined;
      const minAmount = (typeof this.filterMinAmount === 'number' && !isNaN(this.filterMinAmount)) ? this.filterMinAmount : undefined;
      const maxAmount = (typeof this.filterMaxAmount === 'number' && !isNaN(this.filterMaxAmount)) ? this.filterMaxAmount : undefined;
      const customerName = this.filterCustomerId ? (this.customersList.find(c => c.id == this.filterCustomerId)?.name || '') : undefined;
      const variantName = this.filterVariantId ? (this.variantsList.find(v => v.id == this.filterVariantId)?.name || '') : undefined;
      const pageSize = 500;
      let allSales: any[] = [];
      let page = 0;
      let totalPages = 1;
      // Use agencyName fetched from backend
      const businessName = this.agencyName;
      const fetchPage = () => {
        this.saleService.getAllSales(page, pageSize, 'id', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount)
          .pipe(
            catchError((error: any) => {
              const errorMessage = error?.error?.message || error?.message || 'Error loading sales data';
              this.toastr.error(errorMessage, 'Error');
              console.error('Full error:', error);
              return of({ content: [], totalPages: 0 });
            })
          )
          .subscribe((data: any) => {
            const content = data.content || data;
            allSales = allSales.concat(content);
            totalPages = data.totalPages || 1;
            page++;
            if (page < totalPages) {
              fetchPage();
            } else {
              // Fetch backend summary for perfect consistency
              this.saleService.getSalesSummary(fromDate, toDate, customerId, variantId, minAmount, maxAmount)
                .pipe(
                  catchError(() => of({ totalSalesAmount: 0, avgSaleValue: 0, topCustomer: 'N/A', transactionCount: 0 }))
                )
                .subscribe((summary: any) => {
                  exportSalesReportToPDF({
                    salesData: allSales,
                    fromDate: this.filterFromDate,
                    toDate: this.filterToDate,
                    totalSalesAmount: summary.totalSalesAmount,
                    avgSaleValue: summary.avgSaleValue,
                    topCustomer: summary.topCustomer,
                    customerName,
                    variantName,
                    minAmount,
                    maxAmount,
                    transactionCount: summary.transactionCount,
                    businessName
                  });
                  this.toastr.success('PDF exported!', 'Success');
                });
            }
          });
      };
      fetchPage();
    } else {
      this.toastr.info('PDF export is only available for the Sales Report.', 'Info');
    }
  }
}

