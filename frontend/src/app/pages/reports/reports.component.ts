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
import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { catchError, of, finalize } from 'rxjs';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faShoppingCart, faHourglassEnd, faBox, faTruck, faDownload, faReceipt, faFileInvoice, faEye, faBank } from '@fortawesome/free-solid-svg-icons';
import { exportSalesReportToPDF } from './export-sales-report.util';
import { exportDuePaymentReportToPDF } from './export-due-payment-report.util';
import { BusinessInfoService } from '../../services/business-info.service';
import { ToastrService } from 'ngx-toastr';
import { SaleService } from '../../services/sale.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { CustomerDuePaymentService } from '../../services/customer-due-payment.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { SupplierTransactionService } from '../../services/supplier-transaction.service';
import { BankAccountLedgerService } from '../../services/bank-account-ledger.service';
import { BankAccountService } from '../../services/bank-account.service';
import { WarehouseService } from '../../services/warehouse.service';
import { LoadingService } from '../../services/loading.service';
import { ExpenseReportComponent } from '../expenses/expense-report.component';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { AlertSettingsService } from '../../services/alert-settings.service';



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
  styleUrl: './reports.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReportsComponent implements OnInit, OnDestroy {
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
    duePaymentPage = 1;
    duePaymentPageSize = 10;
    duePaymentTotalPages = 1;
    duePaymentTotalElements = 0;

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
      // Sort by sale date descending (latest first), then by ID descending for same-day records
      return items.sort((a, b) => {
        const dateA = new Date(a.sale.saleDate).getTime();
        const dateB = new Date(b.sale.saleDate).getTime();
        if (dateB !== dateA) {
          return dateB - dateA;
        }
        // If dates are the same, sort by ID descending (latest record first)
        return (b.sale.id || 0) - (a.sale.id || 0);
      });
    }
    get paginatedSalesData() {
      // Backend returns SaleDTO objects with nested saleItems
      // We need to flatten them for display: { sale, item } format
      const sales = this.salesData || [];
      const flattened = sales.flatMap(sale =>
        (sale.saleItems || []).map((item: any) => ({ sale, item }))
      );
      return flattened;
    }
    get paginatedReturnPendingData() {
      const startIndex = (this.returnPendingPage - 1) * this.returnPendingPageSize;
      const endIndex = startIndex + this.returnPendingPageSize;
      return this.returnPendingData.slice(startIndex, endIndex);
    }
    get paginatedInventoryData() {
      return this.inventoryData;
    }
    get paginatedSupplierData() {
      return this.supplierData;
    }
    get paginatedDuePaymentData() {
      // Data is already paginated from backend, but sort by ID descending as backup
      return (this.duePaymentData || []).sort((a: any, b: any) => (b.id || 0) - (a.id || 0));
    }
  // Sales filters
  filterCustomerId: string = '';
  filterVariantId: string = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;
  filterSalesReference: string = '';
  filterReturnPendingVariantId: string = '';
  filterInventoryWarehouseId: string = '';
  filterDuePaymentCustomerId: string = '';
  filterDuePaymentMinAmount: number | null = null;
  filterDuePaymentMaxAmount: number | null = null;
  filterPaymentModeCustomerId: string = '';
  filterPaymentModePaymentMode: string = '';
  filterPaymentModeVariantId: string = '';
  filterPaymentModeBankAccountId: string = '';
  filterPaymentModeMinAmount: number | null = null;
  filterPaymentModeMaxAmount: number | null = null;
  filterPaymentModeMinTransactions: number | null = null;
  customersList: any[] = [];
  variantsList: any[] = [];
  warehousesList: any[] = [];
  bankAccountsList: any[] = [];
  paymentModesList: any[] = [];
  selectedReport = 'sales';
  filterFromDate = '';
  filterToDate = '';
  filterDuePaymentFromDate = '';
  filterDuePaymentToDate = '';
  filterPaymentModeFromDate = '';
  filterPaymentModeToDate = '';

  // Font Awesome Icons
  faShoppingCart = faShoppingCart;
  faHourglassEnd = faHourglassEnd;
  faBox = faBox;
  faTruck = faTruck;
  faDownload = faDownload;
  faReceipt = faReceipt;
  faFileInvoice = faFileInvoice;
  faEye = faEye;
  faBank = faBank;

  availableReports: ReportOption[] = [
    { id: 'sales', name: 'Sales Report', icon: faShoppingCart },
    { id: 'expenses', name: 'Expenses Report', icon: faReceipt },
    { id: 'customerDuePayment', name: 'Customer Due Payment', icon: faFileInvoice },
    { id: 'returnPending', name: 'Return Pending', icon: faHourglassEnd },
    { id: 'inventory', name: 'Inventory', icon: faBox },
    { id: 'supplier', name: 'Supplier', icon: faTruck },
    { id: 'paymentModes', name: 'Payment Mode Report', icon: faBank }
  ];

  // Sales Data
  salesData: any[] = [];

  // Return Pending Data
  returnPendingData: any[] = [];
  pendingReturnThreshold: number | null = null; // null means not configured, fallback to 10 for calculations

  // Inventory Data
  inventoryData: any[] = [];

  // Supplier Data
  supplierData: any[] = [];

  // Payment Mode Data
  paymentModesData: any[] = [];
  paymentModesSummary: any = {
    paymentModeStats: {},
    totalAmount: 0,
    totalTransactions: 0
  };

  // Customer Due Payment Data
  duePaymentData: any[] = [];
  duePaymentSummary: any = {
    totalDueAmount: 0,
    totalSalesAmount: 0,
    totalAmountReceived: 0,
    totalCustomersWithDue: 0,
    averageDueAmount: 0
  };

  // Summary card values
  summaryTotalSalesAmount: number = 0;
  summaryTransactionCount: number = 0;
  summaryAvgSaleValue: number = 0;
  summaryTopCustomer: string = 'N/A';

  // Invoice modal properties
  selectedSale: any = null;
  originalSalesMap: Map<number, any> = new Map();

  constructor(
    private saleService: SaleService,
    private duePaymentService: CustomerDuePaymentService,
    private ledgerService: CustomerCylinderLedgerService,
    private inventoryService: InventoryStockService,
    private supplierTransactionService: SupplierTransactionService,
    private bankAccountLedgerService: BankAccountLedgerService,
    private paymentModeService: PaymentModeService,
    private customerService: CustomerService,
    private variantService: CylinderVariantService,
    private warehouseService: WarehouseService,
    private bankAccountService: BankAccountService,
    private toastr: ToastrService,
    private businessInfoService: BusinessInfoService,
    private loadingService: LoadingService,
    private alertSettingsService: AlertSettingsService,
    private cdr: ChangeDetectorRef
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
    
    // Load initial data for sales and return pending reports
    this.applyFilters(false);
    this.loadReturnPendingData();
    this.loadInventoryData();
    this.loadSupplierData();
    
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
    this.warehouseService.getActiveWarehouses().subscribe({
      next: (data: any) => {
        this.warehousesList = data || [];
      },
      error: () => { this.warehousesList = []; }
    });
    
    // Load bank accounts for filtering
    this.bankAccountService.getActiveBankAccounts().subscribe({
      next: (data: any) => {
        this.bankAccountsList = data.content || data || [];
      },
      error: () => { this.bankAccountsList = []; }
    });
    
    // Load all payment modes (active and inactive) for historical filtering
    this.paymentModeService.getAllPaymentModes(0, 100).subscribe({
      next: (data: any) => {
        this.paymentModesList = Array.isArray(data) ? data : (data.content || []);
      },
      error: () => { this.paymentModesList = []; }
    });

    // Load pending return threshold from alert settings
    this.loadPendingReturnThreshold();
  }

  ngOnDestroy() {}

  onTabChange(tabName: string) {
    this.selectedReport = tabName;
    if (this.selectedReport === 'sales') {
      this.applyFilters(false);
    }
    if (this.selectedReport === 'returnPending') {
      // Reload threshold each time the tab is accessed to get latest value
      this.loadPendingReturnThreshold();
      this.loadReturnPendingData();
    }
    if (this.selectedReport === 'inventory') {
      this.loadInventoryData();
    }
    if (this.selectedReport === 'supplier') {
      this.loadSupplierData();
    }
    if (this.selectedReport === 'customerDuePayment') {
      this.loadDuePaymentData();
    }
    if (this.selectedReport === 'paymentModes') {
      this.loadPaymentModesData();
    }
  }

  private loadPendingReturnThreshold(): void {
    this.alertSettingsService.getAlertConfig('PENDING_RETURN_CYLINDERS')
      .subscribe({
        next: (response: any) => {
          console.log('Alert config response:', response);
          if (response?.data && response.data.pendingReturnThreshold !== undefined && response.data.pendingReturnThreshold !== null) {
            this.pendingReturnThreshold = response.data.pendingReturnThreshold;
            console.log('Pending return threshold loaded:', this.pendingReturnThreshold);
            this.cdr.markForCheck();
          } else if (response?.data) {
            console.warn('Pending return threshold not found in config data:', response.data);
            this.pendingReturnThreshold = null;
          } else {
            console.warn('No valid data in alert config response:', response);
            this.pendingReturnThreshold = null;
          }
        },
        error: (err) => {
          console.warn('Could not load pending return threshold:', err);
          this.pendingReturnThreshold = null;
        }
      });
  }

  loadReturnPendingData() {
    // Simulate paging for return pending (if backend supports, update here)
    const sub = this.ledgerService.getAllReturnPendingSummary()
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading return pending data';
          this.toastr.error(errorMessage, 'Error');
          return of([]);
        })
      )
      .subscribe((data: any) => {
        // Store the full transformed data
        this.returnPendingData = data
          .sort((a: any, b: any) => (b.id || 0) - (a.id || 0))
          .map((ledger: any) => ({
            customer: ledger.customerName || 'Unknown',
            variant: ledger.variantName || 'Unknown',
            returnPending: ledger.balance || 0
          }));
        // Calculate pagination based on full data
        this.returnPendingTotalElements = this.returnPendingData.length;
        this.returnPendingTotalPages = Math.ceil(this.returnPendingData.length / this.returnPendingPageSize) || 1;
        this.cdr.markForCheck();
        sub.unsubscribe();
      });
  }

  loadInventoryData() {
    this.loadingService.show('Loading inventory data...');
    
    // Use different API endpoints based on filter
    const apiCall = this.filterInventoryWarehouseId 
      ? this.inventoryService.getStockByWarehouse(parseInt(this.filterInventoryWarehouseId))
      : this.inventoryService.getAllStock(this.inventoryPage - 1, this.inventoryPageSize);
    
    const sub = apiCall
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
        let stockArray = Array.isArray(data) ? data : (data.content || data);
        console.log('Loaded stock array:', stockArray);
        console.log('Filter warehouse ID:', this.filterInventoryWarehouseId);
        
        if (this.filterInventoryWarehouseId) {
          // Get warehouse name from selected warehouse
          const selectedWarehouse = this.warehousesList.find(w => w.id === parseInt(this.filterInventoryWarehouseId));
          const warehouseName = selectedWarehouse ? selectedWarehouse.name : 'Unknown';
          
          // Show individual warehouse items sorted by ID descending
          this.inventoryData = stockArray
            .sort((a: any, b: any) => (b.id || 0) - (a.id || 0))
            .map((stock: any) => ({
              warehouseId: stock.warehouseId,
              warehouse: warehouseName,
              variant: stock.variantName || 'Unknown',
              filled: stock.filledQty || 0,
              empty: stock.emptyQty || 0
            }));
        } else {
          // Group by variant and sum quantities when showing all warehouses, then sort by ID descending
          const groupedByVariant = new Map<string, any>();
          stockArray.forEach((stock: any) => {
            const variantName = stock.variantName || 'Unknown';
            if (!groupedByVariant.has(variantName)) {
              groupedByVariant.set(variantName, {
                warehouse: 'All Warehouses',
                variant: variantName,
                filled: 0,
                empty: 0,
                id: stock.id || 0
              });
            }
            const item = groupedByVariant.get(variantName);
            item.filled += stock.filledQty || 0;
            item.empty += stock.emptyQty || 0;
          });
          this.inventoryData = Array.from(groupedByVariant.values())
            .sort((a: any, b: any) => (b.id || 0) - (a.id || 0));
        }
        
        console.log('Final inventory data:', this.inventoryData);
        this.inventoryTotalElements = this.inventoryData.length;
        this.cdr.markForCheck();
        this.inventoryTotalPages = Math.ceil(this.inventoryData.length / this.inventoryPageSize) || 1;
        sub.unsubscribe();
      });
  }

  onInventoryWarehouseChange() {
    this.inventoryPage = 1;
    this.loadInventoryData();
  }

  resetInventoryFilter() {
    this.filterInventoryWarehouseId = '';
    this.inventoryPage = 1;
    this.loadInventoryData();
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
            supplierMap.set(transaction.supplierId, { 
              filled: 0, 
              empty: 0, 
              name: transaction.supplierName,
              id: transaction.id || 0
            });
          }
          const supplier = supplierMap.get(transaction.supplierId);
          supplier.filled += transaction.filledReceived || 0;
          supplier.empty += transaction.emptySent || 0;
        });
        this.supplierData = Array.from(supplierMap.values())
          .sort((a: any, b: any) => (b.id || 0) - (a.id || 0));
        this.supplierTotalElements = data.totalElements || this.supplierData.length;
        this.cdr.markForCheck();
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
    if (this.pendingReturnThreshold === null) return 0; // No threshold configured, no high risk
    return this.filteredReturnPendingData.filter(item => (Number(item.returnPending) || 0) > (this.pendingReturnThreshold ?? 0)).length;
  }

  getHighRiskReturnPendingCount(): number {
    if (this.pendingReturnThreshold === null) return 0; // No threshold configured, no high risk
    return this.filteredReturnPendingData.filter(item => item.returnPending > (this.pendingReturnThreshold ?? 0)).length;
  }

  getReturnPendingStatus(returnPending: number): string {
    // If no threshold is configured, only show Pending or Clear
    if (this.pendingReturnThreshold === null) {
      return returnPending > 0 ? 'Pending' : 'Clear';
    }
    
    if (returnPending > this.pendingReturnThreshold) {
      return 'High Risk';
    } else if (returnPending > 0) {
      return 'Pending';
    } else {
      return 'Clear';
    }
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
    const fromDate = this.filterFromDate ? this.filterFromDate : undefined;
    const toDate = this.filterToDate ? this.filterToDate : undefined;
    const customerId = this.filterCustomerId ? this.filterCustomerId : undefined;
    const variantId = this.filterVariantId ? Number(this.filterVariantId) : undefined;
    const minAmount = (typeof this.filterMinAmount === 'number' && !isNaN(this.filterMinAmount)) ? this.filterMinAmount : undefined;
    const maxAmount = (typeof this.filterMaxAmount === 'number' && !isNaN(this.filterMaxAmount)) ? this.filterMaxAmount : undefined;
    const referenceNumber = this.filterSalesReference ? this.filterSalesReference : undefined;
    
    // Fetch current page data with backend-driven pagination
    const sub = this.saleService.getAllSales(this.salesPage - 1, this.salesPageSize, 'id', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber)
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
        
        // Store original sales by ID for modal display
        this.originalSalesMap.clear();
        this.salesData.forEach((sale: any) => {
          this.originalSalesMap.set(sale.id, sale);
        });
        
        // Get total pages from backend
        this.salesTotalElements = data.totalElements || 0;
        this.salesTotalPages = data.totalPages || Math.ceil(this.salesTotalElements / this.salesPageSize) || 1;
        
        if (showToastr) {
          this.toastr.info('Filters applied', 'Info');
        }
        this.cdr.markForCheck();
        sub.unsubscribe();
      });
    // Fetch summary card values from backend summary endpoint (for ALL data)
    this.saleService.getSalesSummary(fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber)
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
        this.cdr.markForCheck();
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
      const referenceNumber = this.filterSalesReference ? this.filterSalesReference : undefined;
      const fetchPage = () => {
        this.saleService.getAllSales(page, pageSize, 'id', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber)
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
              this.saleService.getSalesSummary(fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber)
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
                    referenceNumber: this.filterSalesReference,
                    transactionCount: summary.transactionCount,
                    businessName
                  });
                  this.toastr.success('PDF exported!', 'Success');
                });
            }
          });
      };
      fetchPage();
    } else if (this.selectedReport === 'customerDuePayment') {
      // Export Customer Due Payment Report to PDF
      const fromDate = this.filterDuePaymentFromDate ? this.filterDuePaymentFromDate : undefined;
      const toDate = this.filterDuePaymentToDate ? this.filterDuePaymentToDate : undefined;
      const customerId = this.filterDuePaymentCustomerId ? this.filterDuePaymentCustomerId : undefined;      const customerName = this.filterDuePaymentCustomerId ? (this.customersList.find(c => c.id == this.filterDuePaymentCustomerId)?.name || '') : undefined;      const minAmount = (typeof this.filterDuePaymentMinAmount === 'number' && !isNaN(this.filterDuePaymentMinAmount)) ? this.filterDuePaymentMinAmount : undefined;
      const maxAmount = (typeof this.filterDuePaymentMaxAmount === 'number' && !isNaN(this.filterDuePaymentMaxAmount)) ? this.filterDuePaymentMaxAmount : undefined;
      const pageSize = 1000;
      let allDuePayments: any[] = [];
      let page = 0;
      let totalPages = 1;
      const businessName = this.agencyName;

      const fetchDuePaymentPage = () => {
        this.duePaymentService.getDuePaymentReport(page, pageSize, 'dueAmount', 'DESC', fromDate, toDate, customerId, minAmount, maxAmount)
          .pipe(
            catchError((error: any) => {
              const errorMessage = error?.error?.message || error?.message || 'Error loading due payment data';
              this.toastr.error(errorMessage, 'Error');
              console.error('Full error:', error);
              return of({ content: [], totalPages: 0 });
            })
          )
          .subscribe((data: any) => {
            const content = data.content || data;
            allDuePayments = allDuePayments.concat(content);
            totalPages = data.totalPages || 1;
            page++;
            if (page < totalPages) {
              fetchDuePaymentPage();
            } else {
              // Fetch backend summary for perfect consistency
              this.duePaymentService.getDuePaymentReportSummary(fromDate, toDate, customerId, minAmount, maxAmount)
                .pipe(
                  catchError(() => of({
                    totalDueAmount: 0,
                    totalSalesAmount: 0,
                    totalAmountReceived: 0,
                    totalCustomersWithDue: 0,
                    averageDueAmount: 0
                  }))
                )
                .subscribe((summary: any) => {
                  exportDuePaymentReportToPDF({
                    duePaymentData: allDuePayments,
                    fromDate: this.filterDuePaymentFromDate,
                    toDate: this.filterDuePaymentToDate,
                    customerName,
                    totalDueAmount: summary.totalDueAmount,
                    totalSalesAmount: summary.totalSalesAmount,
                    totalAmountReceived: summary.totalAmountReceived,
                    totalCustomersWithDue: summary.totalCustomersWithDue,
                    averageDueAmount: summary.averageDueAmount,
                    minAmount,
                    maxAmount,
                    businessName
                  });
                  this.toastr.success('PDF exported!', 'Success');
                });
            }
          });
      };
      fetchDuePaymentPage();
    } else {
      this.toastr.info('PDF export is not available for this report.', 'Info');
    }
  }

  loadDuePaymentData() {
    this.loadingService.show('Loading due payment data...');
    
    const fromDate = this.filterDuePaymentFromDate || undefined;
    const toDate = this.filterDuePaymentToDate || undefined;
    const customerId = this.filterDuePaymentCustomerId || undefined;
    const minAmount = (typeof this.filterDuePaymentMinAmount === 'number' && !isNaN(this.filterDuePaymentMinAmount)) ? this.filterDuePaymentMinAmount : undefined;
    const maxAmount = (typeof this.filterDuePaymentMaxAmount === 'number' && !isNaN(this.filterDuePaymentMaxAmount)) ? this.filterDuePaymentMaxAmount : undefined;

    // Fetch current page data with backend-driven pagination
    const sub = this.duePaymentService.getDuePaymentReport(
      this.duePaymentPage - 1, this.duePaymentPageSize, 'id', 'DESC', fromDate, toDate, customerId, minAmount, maxAmount
    )
      .pipe(
        finalize(() => this.loadingService.hide()),
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading due payment data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        })
      )
      .subscribe((data: any) => {
        const content = data.content || data;
        this.duePaymentData = content;
        this.duePaymentTotalElements = data.totalElements || content.length;
        this.duePaymentTotalPages = data.totalPages || Math.ceil(this.duePaymentTotalElements / this.duePaymentPageSize) || 1;
        this.cdr.markForCheck();
        sub.unsubscribe();
      });

    // Fetch summary
    this.duePaymentService.getDuePaymentReportSummary(fromDate, toDate, customerId, minAmount, maxAmount)
      .pipe(
        catchError((error: any) => {
          return of({
            totalDueAmount: 0,
            totalSalesAmount: 0,
            totalAmountReceived: 0,
            totalCustomersWithDue: 0,
            averageDueAmount: 0
          });
        })
      )
      .subscribe((summary: any) => {
        this.duePaymentSummary = summary;
        this.cdr.markForCheck();
      });
  }

  onGenerateDuePaymentClick() {
    this.duePaymentPage = 1;
    this.loadDuePaymentData();
  }

  onDuePaymentPageChange(page: number) {
    if (page > 0 && page <= this.duePaymentTotalPages) {
      this.duePaymentPage = page;
      this.loadDuePaymentData();
      this.cdr.markForCheck();
    }
  }

  onDuePaymentPageSizeChange(size: number) {
    this.duePaymentPageSize = size;
    this.duePaymentPage = 1;
    this.loadDuePaymentData();
    this.cdr.markForCheck();
  }

  viewInvoice(saleId: number) {
    const numericId = Number(saleId);
    this.selectedSale = this.originalSalesMap.get(numericId);
  }

  closeInvoice() {
    this.selectedSale = null;
  }

  loadPaymentModesData() {
    this.loadingService.show('Loading payment mode data...');
    const fromDate = this.filterPaymentModeFromDate ? this.filterPaymentModeFromDate : undefined;
    const toDate = this.filterPaymentModeToDate ? this.filterPaymentModeToDate : undefined;
    const customerId = this.filterPaymentModeCustomerId ? this.filterPaymentModeCustomerId : undefined;
    const paymentMode = this.filterPaymentModePaymentMode ? this.filterPaymentModePaymentMode : undefined;
    const variantId = this.filterPaymentModeVariantId ? Number(this.filterPaymentModeVariantId) : undefined;
    const bankAccountId = this.filterPaymentModeBankAccountId ? Number(this.filterPaymentModeBankAccountId) : undefined;
    const minAmount = (typeof this.filterPaymentModeMinAmount === 'number' && !isNaN(this.filterPaymentModeMinAmount)) ? this.filterPaymentModeMinAmount : undefined;
    const maxAmount = (typeof this.filterPaymentModeMaxAmount === 'number' && !isNaN(this.filterPaymentModeMaxAmount)) ? this.filterPaymentModeMaxAmount : undefined;
    const minTransactions = (typeof this.filterPaymentModeMinTransactions === 'number' && !isNaN(this.filterPaymentModeMinTransactions)) ? this.filterPaymentModeMinTransactions : undefined;

    this.saleService.getPaymentModeSummary(fromDate, toDate, customerId, paymentMode, variantId, bankAccountId, minAmount, maxAmount, minTransactions)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading payment mode data';
          this.toastr.error(errorMessage, 'Error');
          return of({ paymentModeStats: {}, totalAmount: 0, totalTransactions: 0 });
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: any) => {
        // Convert payment mode stats object to array for display
        this.paymentModesSummary = data;
        this.paymentModesData = Object.entries(data.paymentModeStats || {}).map(([key, value]: [string, any]) => ({
          paymentMode: key,
          ...value
        }));
        this.cdr.markForCheck();
      });
  }

  onGeneratePaymentModesClick() {
    this.loadPaymentModesData();
  }

  applyPaymentModeFilters() {
    this.loadPaymentModesData();
  }

  resetPaymentModeFilters() {
    this.filterPaymentModeFromDate = '';
    this.filterPaymentModeToDate = '';
    this.filterPaymentModeCustomerId = '';
    this.filterPaymentModePaymentMode = '';
    this.filterPaymentModeVariantId = '';
    this.filterPaymentModeBankAccountId = '';
    this.filterPaymentModeMinAmount = null;
    this.filterPaymentModeMaxAmount = null;
    this.filterPaymentModeMinTransactions = null;
    this.loadPaymentModesData();
  }
}
