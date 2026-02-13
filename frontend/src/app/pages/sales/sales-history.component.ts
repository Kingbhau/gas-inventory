import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList } from '@angular/core';
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
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { Customer } from '../../models/customer.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { PageResponse } from '../../models/page-response';
import { Sale } from '../../models/sale.model';
import { SaleItem } from '../../models/sale-item.model';

type FlattenedSaleRow = {
  id: number;
  referenceNumber: string;
  customerId: number;
  customerName: string;
  saleDate: string;
  totalAmount: number;
  paymentMode: string;
  bankAccountId?: number;
  amountReceived: number;
  bankAccountName?: string;
  variantName?: string;
  filledIssuedQty?: number;
  qtyEmptyReceived?: number;
  basePrice?: number;
  discount?: number;
  finalPrice?: number;
  createdBy?: string;
};

@Component({
  selector: 'app-sales-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './sales-history.component.html',
  styleUrl: './sales-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SalesHistoryComponent implements OnInit, OnDestroy {
  @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
  filterFromDate = '';
  filterToDate = '';
  selectedCustomer = '';
  filterVariantId: string = '';
  filterMinAmount: number | null = null;
  filterMaxAmount: number | null = null;
  filterReference: string = '';
  filterCreatedBy = '';
  variantsList: CylinderVariant[] = [];
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

  allSales: FlattenedSaleRow[] = [];
  filteredSales: FlattenedSaleRow[] = [];
  selectedSale: Sale | null = null;
  customersList: Customer[] = [];
  users: User[] = [];
  originalSalesMap: Map<number, Sale> = new Map();

  constructor(
    private saleService: SaleService,
    private customerService: CustomerService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService,
    private loadingService: LoadingService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadSales();
    this.loadCustomers();
    this.loadUsers();
    // Load all variants (including inactive) for filtering historical sales
    this.variantService.getAllVariantsAll().subscribe({
      next: (data: CylinderVariant[]) => {
        this.variantsList = data || [];
        this.cdr.markForCheck();
      },
      error: (_error: unknown) => {
        this.variantsList = [];
      }
    });
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
    this.saleService.getAllSales(this.currentPage - 1, this.pageSize, 'saleDate', 'DESC', fromDate, toDate, customerId, variantId, minAmount, maxAmount, referenceNumber, this.filterCreatedBy || undefined)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: PageResponse<Sale>) => {
          // Flatten sales with items into single rows
          const flattenedSales: FlattenedSaleRow[] = [];
          let salesArray: Sale[] = data.items || [];
          if (customerId) {
            salesArray = salesArray.filter((sale) => sale.customerId?.toString() === customerId?.toString());
          }
          salesArray.forEach((sale) => {
            const saleId = sale.id ?? 0;
            if (!saleId) {
              return;
            }
            if (sale.saleItems && sale.saleItems.length > 0) {
              sale.saleItems.forEach((item) => {
                const finalPrice = typeof item.finalPrice === 'number' ? item.finalPrice : Number(item.finalPrice);
                if (
                  (variantId !== undefined && variantId !== null && item.variantId !== variantId) ||
                  (minAmount !== undefined && minAmount !== null && finalPrice < minAmount) ||
                  (maxAmount !== undefined && maxAmount !== null && finalPrice > maxAmount)
                ) {
                  return;
                }
                flattenedSales.push({
                  id: saleId,
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
                  finalPrice: finalPrice,
                  amountReceived: sale.amountReceived,
                  createdBy: sale.createdBy
                });
              });
            } else {
              // Handle sales without items
              flattenedSales.push({
                id: saleId,
                referenceNumber: sale.referenceNumber,
                customerId: sale.customerId,
                customerName: sale.customerName,
                saleDate: sale.saleDate,
                totalAmount: sale.totalAmount,
                paymentMode: sale.paymentMode,
                bankAccountId: sale.bankAccountId,
                bankAccountName: sale.bankAccountName,
                variantName: 'N/A',
                filledIssuedQty: 0,
                amountReceived: sale.amountReceived,
                createdBy: sale.createdBy
              });
            }
          });
          this.allSales = flattenedSales.sort((a, b) => b.id - a.id);
          this.filteredSales = [...this.allSales];
          this.totalElements = data.totalElements ?? this.filteredSales.length;
          this.totalPages = data.totalPages ?? 1;
          // Store original sales for modal display
          this.originalSalesMap.clear();
          salesArray.forEach((sale) => {
            if (sale.id) {
              this.originalSalesMap.set(sale.id, sale);
            }
          });
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading sales';
          this.toastr.error(errorMessage, 'Error');
        }
      });
  }

  loadCustomers() {
    this.loadingService.show('Loading customers...');
    this.customerService.getAllCustomersAll()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: Customer[]) => {
          this.customersList = data;
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading customers';
          this.toastr.error(errorMessage, 'Error');
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
    this.filterCreatedBy = '';
    if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
      this.autocompleteInputs.forEach(input => input.resetInput());
    }
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

  viewInvoice(saleId: number) {
    const numericId = Number(saleId);
    this.selectedSale = this.originalSalesMap.get(numericId) || null;
  }

  closeInvoice() {
    this.selectedSale = null;
  }

  printInvoice() {
    window.print();
  }

  getCreatedByName(createdBy?: string | null): string {
    if (!createdBy) {
      return 'N/A';
    }
    const user = this.users.find(u => u.username === createdBy);
    return user?.name || createdBy;
  }

  private loadUsers() {
    this.userService.getUsers()
      .subscribe({
        next: (users) => {
          this.users = users || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.users = [];
        }
      });
  }
}
