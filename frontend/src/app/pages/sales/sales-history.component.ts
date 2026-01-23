import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faClipboard } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SaleService } from '../../services/sale.service';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-sales-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './sales-history.component.html',
  styleUrl: './sales-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SalesHistoryComponent implements OnInit, OnDestroy {
  filterFromDate = '';
  filterToDate = '';
  selectedCustomer = '';
  filterVariantId: string = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;
  filterReference: string = '';
  variantsList: any[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  totalElements = 0;
  pageSizeOptions = [5, 10, 20, 50, 100];

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 1;
    this.loadSales();
  }

  // Font Awesome Icons
  faEye = faEye;
  faClipboard = faClipboard;

  allSales: any[] = [];
  filteredSales: any[] = [];
  selectedSale: any = null;
  customersList: any[] = [];
  originalSalesMap: Map<number, any> = new Map();

  constructor(
    private saleService: SaleService,
    private customerService: CustomerService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadSales();
    this.loadCustomers();
    // Load all variants (including inactive) for filtering historical sales
    if (this.variantService && this.variantService.getAllVariants) {
      this.variantService.getAllVariants(0, 100).subscribe({
        next: (data: any) => {
          this.variantsList = data.content || data;
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          this.variantsList = [];
        }
      });
    }
  }

  ngOnDestroy() {}

  loadSales() {
    // Use filters if set
    const fromDate = this.filterFromDate || undefined;
    const toDate = this.filterToDate || undefined;
    const customerId = this.selectedCustomer || undefined;
    const variantId = this.filterVariantId ? Number(this.filterVariantId) : undefined;
    const minAmount = (typeof this.filterMinAmount === 'number' && !isNaN(this.filterMinAmount)) ? this.filterMinAmount : undefined;
    const maxAmount = (typeof this.filterMaxAmount === 'number' && !isNaN(this.filterMaxAmount)) ? this.filterMaxAmount : undefined;
    const referenceNumber = this.filterReference ? this.filterReference : undefined;
    this.loadingService.show('Loading sales...');
    this.saleService.getAllSales(this.currentPage - 1, this.pageSize, 'saleDate', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          // Flatten sales with items into single rows
          const flattenedSales: any[] = [];
          let salesArray = data.content || data;
          if (customerId) {
            salesArray = salesArray.filter((sale: any) => sale.customerId?.toString() === customerId?.toString());
          }
          salesArray.forEach((sale: any) => {
            if (sale.saleItems && sale.saleItems.length > 0) {
              sale.saleItems.forEach((item: any) => {
                const finalPrice = typeof item.finalPrice === 'number' ? item.finalPrice : Number(item.finalPrice);
                if (
                  (variantId !== undefined && variantId !== null && item.variantId !== variantId) ||
                  (minAmount !== undefined && minAmount !== null && finalPrice < minAmount) ||
                  (maxAmount !== undefined && maxAmount !== null && finalPrice > maxAmount)
                ) {
                  return;
                }
                flattenedSales.push({
                  id: sale.id,
                  referenceNumber: sale.referenceNumber,
                  customerId: sale.customerId,
                  customerName: sale.customerName,
                  saleDate: sale.saleDate,
                  totalAmount: sale.totalAmount,
                  paymentMode: sale.paymentMode,
                  bankAccountId: sale.bankAccountId,
                  bankAccountName: sale.bankAccountName,
                  variantName: item.variantName,
                  filledIssuedQty: item.qtyIssued,
                  qtyEmptyReceived: item.qtyEmptyReceived,
                  basePrice: item.basePrice,
                  discount: item.discount,
                  finalPrice: finalPrice
                });
              });
            } else {
              // Handle sales without items
              flattenedSales.push({
                id: sale.id,
                referenceNumber: sale.referenceNumber,
                customerId: sale.customerId,
                customerName: sale.customerName,
                saleDate: sale.saleDate,
                totalAmount: sale.totalAmount,
                paymentMode: sale.paymentMode,
                bankAccountId: sale.bankAccountId,
                bankAccountName: sale.bankAccountName,
                variantName: 'N/A',
                filledIssuedQty: 0
              });
            }
          });
          this.allSales = flattenedSales.sort((a: any, b: any) => b.id - a.id);
          this.filteredSales = [...this.allSales];
          this.totalElements = data.totalElements || this.filteredSales.length;
          this.totalPages = data.totalPages || 1;
          // Store original sales for modal display
          this.originalSalesMap.clear();
          salesArray.forEach((sale: any) => {
            this.originalSalesMap.set(sale.id, sale);
          });
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading sales';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadCustomers() {
    this.loadingService.show('Loading customers...');
    this.customerService.getActiveCustomers()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.customersList = data;
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading customers';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  applyFilters() {
    this.currentPage = 1;
    this.loadSales();
  }

  resetFilters() {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.selectedCustomer = '';
    this.filterVariantId = '';
    this.filterMinAmount = null;
    this.filterMaxAmount = null;
    this.filterReference = '';
    this.currentPage = 1;
    this.loadSales();
  }

  get paginatedSales() {
    // Now filteredSales is already paged from backend
    return this.filteredSales;
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadSales();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadSales();
    }
  }

  viewInvoice(saleId: string) {
    const numericId = Number(saleId);
    this.selectedSale = this.originalSalesMap.get(numericId) || this.filteredSales.find(s => s.id === saleId);
  }

  closeInvoice() {
    this.selectedSale = null;
  }

  printInvoice() {
    window.print();
  }
}
