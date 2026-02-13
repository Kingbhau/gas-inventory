import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEdit, faTrash, faBox, faDownload, faEye } from '@fortawesome/free-solid-svg-icons';
import { exportSupplierTransactionsToPDF } from '../reports/export-supplier-transactions.util';
import { BusinessInfoService } from '../../services/business-info.service';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../services/supplier.service';
import { SupplierTransactionService } from '../../services/supplier-transaction.service';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { WarehouseService } from '../../services/warehouse.service';
import { LoadingService } from '../../services/loading.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { finalize, Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { Supplier } from '../../models/supplier.model';
import { CylinderVariant } from '../../models/cylinder-variant.model';
import { Warehouse } from '../../models/warehouse.model';
import { SupplierTransaction } from '../../models/supplier-transaction.model';
import { InventoryStock } from '../../models/inventory-stock.model';
import { PageResponse } from '../../models/page-response';
import { CreateSupplierTransactionRequest } from '../../models/create-supplier-transaction-request.model';

@Component({
  selector: 'app-supplier-transaction',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './supplier-transaction.component.html',
  styleUrl: './supplier-transaction.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierTransactionComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  agencyName: string = '';
    // Pagination state for supplier transactions table
    transactionPage = 1;
    transactionPageSize = 10;
    totalTransactions = 0;
    totalPages = 1;

    getTotalPages() {
      return this.totalPages;
    }
    get paginatedTransactions() {
      return this.filteredTransactions;
    }
  showForm = false;
  editingId: number | null = null;
  selectedSupplier = '';
  selectedWarehouse = '';
  filterFromDate: string = '';
  filterToDate: string = '';
  filterVariantId: string = '';
  filterReference: string = '';
  filterCreatedBy = '';
  selectedTransaction: SupplierTransaction | null = null;
  originalTransaction: SupplierTransaction | null = null;
  transactionForm!: FormGroup;
  currentEmpty: number | null = null;
  emptyLoading = false;
  emptyError = '';

  // Font Awesome Icons
  faEdit = faEdit;
  faTrash = faTrash;
  faBox = faBox;
  faDownload = faDownload;
  faEye = faEye;


  suppliers: Supplier[] = [];
  variants: CylinderVariant[] = [];
  variantsActive: CylinderVariant[] = [];
  warehouses: Warehouse[] = [];
  warehousesActive: Warehouse[] = [];
  allTransactions: SupplierTransaction[] = [];
  filteredTransactions: SupplierTransaction[] = [];
  users: User[] = [];

  warehouseCompare = (w1: Warehouse | number | null, w2: Warehouse | number | null) => {
    if (!w1 || !w2) return w1 === w2;
    const id1 = typeof w1 === 'object' ? w1.id : w1;
    const id2 = typeof w2 === 'object' ? w2.id : w2;
    return id1 === id2;
  };

  getSelectedWarehouseName(): string {
    const warehouseId = this.transactionForm.get('warehouseId')?.value;
    if (!warehouseId) return 'No Warehouse Selected';
    const warehouse = this.warehouses.find(w => w.id === warehouseId);
    return warehouse ? warehouse.name : 'No Warehouse Selected';
  }

  constructor(
    private fb: FormBuilder,
    private supplierService: SupplierService,
    private transactionService: SupplierTransactionService,
    private variantService: CylinderVariantService,
    private warehouseService: WarehouseService,
    private toastr: ToastrService,
    private inventoryStockService: InventoryStockService,
    private businessInfoService: BusinessInfoService,
    private loadingService: LoadingService,
    private dateUtility: DateUtilityService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.businessInfoService.getBusinessInfoById(1).subscribe({
      next: (data) => {
        this.agencyName = data.agencyName || '';
      },
      error: () => {
        this.agencyName = '';
      }
    });
    this.loadWarehouses();
    this.loadSuppliers();
    this.loadVariants();
    this.loadUsers();
    this.loadTransactions();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  exportSupplierTransactionsPDF() {
    if (this.filteredTransactions.length === 0) {
      this.toastr.info('No data to export');
      return;
    }
    const supplierId = this.selectedSupplier ? Number(this.selectedSupplier) : null;
    const variantId = this.filterVariantId ? Number(this.filterVariantId) : null;
    const supplierName = supplierId ? (this.suppliers.find(s => s.id === supplierId)?.name || '') : undefined;
    const variantName = variantId ? (this.variants.find(v => v.id === variantId)?.name || '') : undefined;
    exportSupplierTransactionsToPDF({
      transactions: this.filteredTransactions,
      fromDate: this.filterFromDate,
      toDate: this.filterToDate,
      businessName: this.agencyName,
      supplierName,
      variantName,
      referenceNumber: this.filterReference
    });
    this.toastr.success('PDF exported!', 'Success');
  }

  loadWarehouses() {
    this.loadingService.show('Loading warehouses...');
    this.warehouseService.getAllWarehouses()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: Warehouse[]) => {
          this.warehouses = data || [];
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading warehouses';
          this.toastr.error(errorMessage, 'Error');
        }
      });

    this.warehouseService.getActiveWarehouses()
      .subscribe({
        next: (data: Warehouse[]) => {
          this.warehousesActive = data || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.warehousesActive = [];
        }
      });
  }

  loadSuppliers() {
    this.loadingService.show('Loading suppliers...');
    this.supplierService.getAllSuppliersAll()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: Supplier[]) => {
          this.suppliers = Array.isArray(data) ? data : [];
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading suppliers';
          this.toastr.error(errorMessage, 'Error');
        }
      });
  }

  loadVariants() {
    this.loadingService.show('Loading variants...');
    this.variantService.getAllVariantsAll()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: CylinderVariant[]) => {
          this.variants = data || [];
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading variants';
          this.toastr.error(errorMessage, 'Error');
        }
      });

    this.variantService.getActiveVariants()
      .subscribe({
        next: (data: CylinderVariant[]) => {
          this.variantsActive = data || [];
          this.cdr.markForCheck();
        },
        error: () => {
          this.variantsActive = [];
        }
      });
  }

  getVariantOptions(): CylinderVariant[] {
    return this.editingId ? this.variants : this.variantsActive;
  }

  loadTransactions() {
    this.loadingService.show('Loading transactions...');
    this.transactionService.getAllTransactions(this.transactionPage - 1, this.transactionPageSize, 'id', 'ASC', this.filterReference, this.filterCreatedBy || undefined)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: PageResponse<SupplierTransaction>) => {
          this.allTransactions = (data.items || [])
            .filter((t): t is SupplierTransaction & { id: number } => typeof t.id === 'number')
            .sort((a, b) => b.id - a.id);
          this.filteredTransactions = this.applyCreatedByFilter([...this.allTransactions]);
          this.totalTransactions = this.filterCreatedBy ? this.filteredTransactions.length : (data.totalElements ?? this.allTransactions.length);
          this.totalPages = this.filterCreatedBy ? 1 : (data.totalPages ?? 1);
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error loading transactions';
          this.toastr.error(errorMessage, 'Error');
        }
      });
  }

  onPageChange(page: number) {
    this.transactionPage = page;
    this.loadTransactions();
  }

  onPageSizeChange(size: number) {
    this.transactionPageSize = size;
    this.transactionPage = 1;
    this.loadTransactions();
  }

  initForm() {
    this.transactionForm = this.fb.group({
      warehouseId: ['', Validators.required],
      supplierId: ['', Validators.required],
      variantId: ['', Validators.required],
      transactionDate: ['', Validators.required],
      filledReceived: [0, [Validators.required, Validators.min(0)]],
      emptySent: [0, [Validators.required, Validators.min(0)]],
      reference: [''],
      amount: [0, [Validators.required, Validators.min(0)]]
    });

    this.transactionForm.get('warehouseId')?.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.loadCurrentEmpty());

    this.transactionForm.get('variantId')?.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.loadCurrentEmpty());
  }

  openAddForm() {
    this.editingId = null;
    this.originalTransaction = null;
    this.transactionForm.reset({ supplierId: '', variantId: '' });
    this.showForm = true;
    // Refresh variants to get any newly activated ones
    this.variantService.invalidateCache();
    this.loadVariants();
    this.loadCurrentEmpty();
    this.cdr.markForCheck();
  }

  editTransaction(transaction: SupplierTransaction) {
    this.editingId = transaction.id ?? null;
    this.originalTransaction = { ...transaction };
    const warehouseId = transaction.warehouseId ?? (this.warehouses.length > 0 ? this.warehouses[0].id : null);
    this.transactionForm.patchValue({
      warehouseId: warehouseId,
      supplierId: transaction.supplierId,
      variantId: transaction.variantId,
      transactionDate: transaction.transactionDate,
      filledReceived: transaction.filledReceived,
      emptySent: transaction.emptySent,
      reference: transaction.reference,
      amount: transaction.amount || 0
    });
    this.showForm = true;
    this.loadCurrentEmpty();
    this.cdr.markForCheck();
  }

  saveTransaction() {
    if (!this.transactionForm.valid) return;
    const formData: CreateSupplierTransactionRequest = { ...this.transactionForm.value };
    formData.warehouseId = Number(formData.warehouseId);
    formData.supplierId = Number(formData.supplierId);
    formData.variantId = Number(formData.variantId);
    formData.transactionDate = formData.transactionDate ? formData.transactionDate : this.dateUtility.getTodayInIST();
    formData.filledReceived = Number(formData.filledReceived);
    formData.emptySent = Number(formData.emptySent);

    // Prevent both fields being zero
    if (formData.filledReceived === 0 && formData.emptySent === 0) {
      this.toastr.error('At least one of Filled Received or Empty Sent must be greater than zero.', 'Validation Error');
      return;
    }

    // Prevent sending more empty than available in inventory (warehouse-specific)
    if (formData.emptySent > 0) {
      this.inventoryStockService.getStockByWarehouse(formData.warehouseId).subscribe((stocks: InventoryStock[]) => {
        const stockForVariant = (stocks || []).find((s) => s.variantId === formData.variantId);
        const currentEmpty = stockForVariant?.emptyQty ?? 0;
        const originalEmptySent = this.editingId ? (this.originalTransaction?.emptySent ?? 0) : 0;
        // When editing, allow using the empties already accounted for in this transaction.
        const availableEmpty = currentEmpty + originalEmptySent;
        if (formData.emptySent > availableEmpty) {
          this.toastr.error('Cannot send more empty cylinders than available in inventory for this warehouse.', 'Validation Error');
          return;
        } else {
          this._submitTransaction(formData);
        }
      }, (err: unknown) => {
        const errorObj = err as { error?: { message?: string }; message?: string };
        const errorMessage = errorObj?.error?.message || errorObj?.message || 'Could not validate inventory. Please try again.';
        this.toastr.error(errorMessage, 'Error');
      });
    } else {
      this._submitTransaction(formData);
    }
  }

  loadCurrentEmpty() {
    const warehouseId = this.transactionForm.get('warehouseId')?.value;
    const variantId = this.transactionForm.get('variantId')?.value;
    if (!warehouseId || !variantId) {
      this.currentEmpty = null;
      this.emptyLoading = false;
      this.emptyError = '';
      return;
    }
    this.emptyLoading = true;
    this.emptyError = '';
    this.inventoryStockService.getStockByWarehouse(Number(warehouseId)).subscribe({
      next: (stocks: InventoryStock[]) => {
        const stockForVariant = (stocks || []).find((s) => s.variantId === Number(variantId));
        const currentEmpty = stockForVariant?.emptyQty ?? 0;
        const originalEmptySent = this.editingId ? (this.originalTransaction?.emptySent ?? 0) : 0;
        // Show available empties (include original sent when editing)
        this.currentEmpty = currentEmpty + originalEmptySent;
        this.emptyLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.emptyError = 'Current empty unavailable';
        this.currentEmpty = null;
        this.emptyLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  _submitTransaction(formData: CreateSupplierTransactionRequest & { supplierName?: string; variantName?: string }) {
    // Get supplier name from suppliers array
    const supplier = this.suppliers.find(s => s.id === formData.supplierId);
    if (supplier) {
      formData.supplierName = supplier.name;
    }
    // Get variant name from variants array
    const variant = this.variants.find(v => v.id === formData.variantId);
    if (variant) {
      formData.variantName = variant.name;
    }
    if (this.editingId) {
      const id = this.editingId;
      if (id === null) {
        return;
      }
      this.transactionService.updateTransaction(id, formData).subscribe({
        next: () => {
          this.toastr.success('Transaction updated successfully', 'Success');
          this.loadTransactions();
          this.closeForm();
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          const errorMessage = err?.error?.message || err?.message || 'Error updating transaction';
          this.toastr.error(errorMessage, 'Error');
        }
      });
    } else {
      this.transactionService.recordTransaction(formData).subscribe({
        next: () => {
          this.toastr.success('Transaction created successfully', 'Success');
          this.loadTransactions();
          this.closeForm();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error creating transaction';
          this.toastr.error(errorMessage, 'Error');
        }
      });
    }
  }


  applyFilters() {
    let filtered = [...this.allTransactions];
    // Supplier filter
    if (this.selectedSupplier) {
      const selectedSupplierId = parseInt(this.selectedSupplier);
      filtered = filtered.filter(t => t.supplierId === selectedSupplierId);
    }
    // Warehouse filter
    if (this.selectedWarehouse) {
      const warehouseId = parseInt(this.selectedWarehouse);
      filtered = filtered.filter(t => t.warehouseId === warehouseId);
    }
    // Variant filter
    if (this.filterVariantId) {
      const variantId = parseInt(this.filterVariantId);
      filtered = filtered.filter(t => t.variantId === variantId);
    }
    // Date range filter
    if (this.filterFromDate) {
      filtered = filtered.filter(t => t.transactionDate >= this.filterFromDate);
    }
    if (this.filterToDate) {
      filtered = filtered.filter(t => t.transactionDate <= this.filterToDate);
    }
    // Reference filter
    if (this.filterReference) {
      const refFilter = this.filterReference.toLowerCase();
      filtered = filtered.filter(t => t.reference && t.reference.toLowerCase().includes(refFilter));
    }
    // Created by filter
    if (this.filterCreatedBy) {
      filtered = filtered.filter(t => t.createdBy === this.filterCreatedBy);
    }
    this.filteredTransactions = filtered
      .filter((t): t is SupplierTransaction & { id: number } => typeof t.id === 'number')
      .sort((a, b) => b.id - a.id);
    this.cdr.markForCheck();
  }

  resetFilters() {
    this.selectedSupplier = '';
    this.selectedWarehouse = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterVariantId = '';
    this.filterReference = '';
    this.filterCreatedBy = '';
    this.filteredTransactions = [...this.allTransactions]
      .filter((t): t is SupplierTransaction & { id: number } => typeof t.id === 'number')
      .sort((a, b) => b.id - a.id);
    this.cdr.markForCheck();
  }

  closeForm() {
    this.showForm = false;
    this.editingId = null;
    this.cdr.markForCheck();
  }

  viewTransactionDetails(transaction: SupplierTransaction) {
    this.selectedTransaction = transaction;
    this.cdr.markForCheck();
  }

  closeTransactionDetails() {
    this.selectedTransaction = null;
    this.cdr.markForCheck();
  }

  getCreatedByName(createdBy?: string | null): string {
    if (!createdBy) {
      return 'N/A';
    }
    const user = this.users.find(u => u.username === createdBy);
    return user?.name || createdBy;
  }

  private applyCreatedByFilter(entries: SupplierTransaction[]): SupplierTransaction[] {
    if (!this.filterCreatedBy) {
      return entries;
    }
    return entries.filter(entry => entry.createdBy === this.filterCreatedBy);
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
