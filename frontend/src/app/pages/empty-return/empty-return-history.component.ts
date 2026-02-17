import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList } from '@angular/core';
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
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { Customer } from '../../models/customer.model';
import { CustomerCylinderLedger } from '../../models/customer-cylinder-ledger.model';
import { PageResponse } from '../../models/page-response';

type EmptyReturnRow = CustomerCylinderLedger & {
  id: number;
  variantId: number;
  refType: string;
  refId: number;
  filledOut: number;
  balance: number;
};

@Component({
  selector: 'app-empty-return-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './empty-return-history.component.html',
  styleUrl: './empty-return-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyReturnHistoryComponent implements OnInit, OnDestroy {
  @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
  filterFromDate = '';
  filterToDate = '';
  selectedCustomer = '';
  filterVariantId: string = '';
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
    this.loadEmptyReturns();
  }

  // Font Awesome Icons
  faEye = faEye;
  faClipboard = faClipboard;

  allEmptyReturns: EmptyReturnRow[] = [];
  filteredEmptyReturns: EmptyReturnRow[] = [];
  selectedEmptyReturn: EmptyReturnRow | null = null;
  customersList: Customer[] = [];
  users: User[] = [];
  originalEmptyReturnsMap: Map<number, EmptyReturnRow> = new Map();
  isStaff = false;

  constructor(
    private ledgerService: CustomerCylinderLedgerService,
    private customerService: CustomerService,
    private toastr: ToastrService,
    private variantService: CylinderVariantService,
    private loadingService: LoadingService,
    private userService: UserService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const role = this.authService.getUserInfo()?.role || '';
    this.isStaff = role === 'STAFF';
    this.loadEmptyReturns();
    this.loadCustomers();
    if (!this.isStaff) {
      this.loadUsers();
    }
    // Load all variants (including inactive) for filtering historical empty returns
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

  loadEmptyReturns() {
    const fromDate = this.filterFromDate || undefined;
    const toDate = this.filterToDate || undefined;
    const customerId = this.selectedCustomer ? this.selectedCustomer.toString() : undefined;
    const variantId = this.filterVariantId ? this.filterVariantId : undefined;
    
    this.loadingService.show('Loading empty returns...');
    this.ledgerService.getEmptyReturns(this.currentPage - 1, this.pageSize, 'transactionDate', 'DESC', fromDate, toDate, customerId, variantId, this.filterCreatedBy || undefined)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: PageResponse<CustomerCylinderLedger>) => {
          const emptyReturns: EmptyReturnRow[] = [];
          const emptyReturnsArray = data.items || [];
          
          emptyReturnsArray.forEach((entry) => {
            if (entry.id === undefined) {
              return;
            }
            emptyReturns.push({
              id: entry.id,
              transactionReference: entry.transactionReference,
              customerId: entry.customerId,
              customerName: entry.customerName,
              transactionDate: entry.transactionDate,
              variantName: entry.variantName,
              variantId: entry.variantId ?? 0,
              refType: entry.refType ?? 'EMPTY_RETURN',
              refId: entry.refId ?? 0,
              filledOut: entry.filledOut ?? 0,
              emptyIn: entry.emptyIn,
              balance: entry.balance ?? 0,
              amountReceived: entry.amountReceived,
              paymentMode: entry.paymentMode,
              dueAmount: entry.dueAmount,
              bankAccountName: entry.bankAccountName,
              bankAccountNumber: entry.bankAccountNumber,
              createdBy: entry.createdBy
            });
          });

          this.allEmptyReturns = emptyReturns.sort((a, b) => b.id - a.id);
          this.filteredEmptyReturns = [...this.allEmptyReturns];
          this.totalElements = data.totalElements ?? this.filteredEmptyReturns.length;
          this.totalPages = data.totalPages ?? 1;
          
          // Store original empty returns for modal display
          this.originalEmptyReturnsMap.clear();
          emptyReturnsArray.forEach((entry) => {
            if (entry.id !== undefined) {
              this.originalEmptyReturnsMap.set(entry.id, entry as EmptyReturnRow);
            }
          });
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading empty returns';
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
    this.loadEmptyReturns();
  }

  resetFilters() {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.selectedCustomer = '';
    this.filterVariantId = '';
    this.filterCreatedBy = '';
    if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
      this.autocompleteInputs.forEach(input => input.resetInput());
    }
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

  viewDetails(emptyReturnId: number) {
    this.selectedEmptyReturn = this.originalEmptyReturnsMap.get(emptyReturnId)
      || this.filteredEmptyReturns.find(e => e.id === emptyReturnId)
      || null;
  }

  closeDetails() {
    this.selectedEmptyReturn = null;
  }

  printDetails() {
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
