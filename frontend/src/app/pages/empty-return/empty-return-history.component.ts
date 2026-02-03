import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faClipboard } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { CustomerService } from '../../services/customer.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { LoadingService } from '../../services/loading.service';
import { CustomerCylinderLedgerService } from '../../services/customer-cylinder-ledger.service';
import { finalize } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-empty-return-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './empty-return-history.component.html',
  styleUrl: './empty-return-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyReturnHistoryComponent implements OnInit, OnDestroy {
  filterFromDate = '';
  filterToDate = '';
  selectedCustomer = '';
  filterVariantId: string = '';
  variantsList: any[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  totalElements = 0;
  pageSizeOptions = [5, 10, 20, 50, 100];

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 1;
    this.loadEmptyReturns();
  }

  // Font Awesome Icons
  faEye = faEye;
  faClipboard = faClipboard;

  allEmptyReturns: any[] = [];
  filteredEmptyReturns: any[] = [];
  selectedEmptyReturn: any = null;
  customersList: any[] = [];
  originalEmptyReturnsMap: Map<number, any> = new Map();

  constructor(
    private ledgerService: CustomerCylinderLedgerService,
    private customerService: CustomerService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadEmptyReturns();
    this.loadCustomers();
    // Load all variants (including inactive) for filtering historical empty returns
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

  loadEmptyReturns() {
    const fromDate = this.filterFromDate || undefined;
    const toDate = this.filterToDate || undefined;
    const customerId = this.selectedCustomer ? this.selectedCustomer.toString() : undefined;
    const variantId = this.filterVariantId ? this.filterVariantId : undefined;
    
    this.loadingService.show('Loading empty returns...');
    this.ledgerService.getEmptyReturns(this.currentPage - 1, this.pageSize, 'transactionDate', 'DESC', fromDate, toDate, customerId, variantId)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          const emptyReturns: any[] = [];
          let emptyReturnsArray = data.content || data;
          
          emptyReturnsArray.forEach((entry: any) => {
            emptyReturns.push({
              id: entry.id,
              transactionReference: entry.transactionReference,
              customerId: entry.customerId,
              customerName: entry.customerName,
              transactionDate: entry.transactionDate,
              variantName: entry.variantName,
              emptyIn: entry.emptyIn,
              amountReceived: entry.amountReceived,
              paymentMode: entry.paymentMode,
              dueAmount: entry.dueAmount,
              bankAccountName: entry.bankAccountName,
              bankAccountNumber: entry.bankAccountNumber
            });
          });

          this.allEmptyReturns = emptyReturns.sort((a: any, b: any) => b.id - a.id);
          this.filteredEmptyReturns = [...this.allEmptyReturns];
          this.totalElements = data.totalElements || this.filteredEmptyReturns.length;
          this.totalPages = data.totalPages || 1;
          
          // Store original empty returns for modal display
          this.originalEmptyReturnsMap.clear();
          emptyReturnsArray.forEach((entry: any) => {
            this.originalEmptyReturnsMap.set(entry.id, entry);
          });
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading empty returns';
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
    this.loadEmptyReturns();
  }

  resetFilters() {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.selectedCustomer = '';
    this.filterVariantId = '';
    this.currentPage = 1;
    this.loadEmptyReturns();
  }

  get paginatedEmptyReturns() {
    return this.filteredEmptyReturns;
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadEmptyReturns();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadEmptyReturns();
    }
  }

  viewDetails(emptyReturnId: string) {
    const numericId = Number(emptyReturnId);
    this.selectedEmptyReturn = this.originalEmptyReturnsMap.get(numericId) || this.filteredEmptyReturns.find(e => e.id === emptyReturnId);
  }

  closeDetails() {
    this.selectedEmptyReturn = null;
  }

  printDetails() {
    window.print();
  }
}
