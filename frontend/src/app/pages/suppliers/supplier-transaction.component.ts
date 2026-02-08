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
import { UserService, User } from '../../services/user.service';

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
  editingId: string | null = null;
  selectedSupplier = '';
  selectedWarehouse = '';
  filterFromDate: string = '';
  filterToDate: string = '';
  filterVariantId: string = '';
  filterReference: string = '';
  filterCreatedBy = '';
  selectedTransaction: any = null;
  originalTransaction: any = null;
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


  suppliers: any[] = [];
  variants: any[] = [];
  warehouses: any[] = [];
  allTransactions: any[] = [];
  filteredTransactions: any[] = [];
  users: User[] = [];

  warehouseCompare = (w1: any, w2: any) => {
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
    const supplierName = this.selectedSupplier ? (this.suppliers.find(s => s.id == this.selectedSupplier)?.name || '') : undefined;
    const variantName = this.filterVariantId ? (this.variants.find(v => v.id == this.filterVariantId)?.name || '') : undefined;
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
    this.warehouseService.getActiveWarehouses()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: any) => {
          this.warehouses = data || [];
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading warehouses';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadSuppliers() {
    this.loadingService.show('Loading suppliers...');
    this.supplierService.getAllSuppliersAll()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: any) => {
          this.suppliers = Array.isArray(data) ? data : (data?.content || []);
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading suppliers';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadVariants() {
    this.loadingService.show('Loading variants...');
    this.variantService.getActiveVariants()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.variants = data;
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading variants';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
  }

  loadTransactions() {
    this.loadingService.show('Loading transactions...');
    this.transactionService.getAllTransactions(this.transactionPage - 1, this.transactionPageSize, 'id', 'ASC', this.filterReference, this.filterCreatedBy || undefined)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.allTransactions = (data.content || data).sort((a: any, b: any) => b.id - a.id);
          console.log('Loaded transactions:', this.allTransactions); // Debug
          this.filteredTransactions = this.applyCreatedByFilter([...this.allTransactions]);
          this.totalTransactions = this.filterCreatedBy ? this.filteredTransactions.length : (data.totalElements || this.allTransactions.length);
          this.totalPages = this.filterCreatedBy ? 1 : (data.totalPages || 1);
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading transactions';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
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

  editTransaction(transaction: any) {
    this.editingId = transaction.id;
    this.originalTransaction = { ...transaction };
    const warehouseId = transaction.warehouseId ? parseInt(transaction.warehouseId) : (this.warehouses.length > 0 ? this.warehouses[0].id : null);
    console.log('Editing transaction:', transaction); // Debug
    console.log('Setting warehouseId to:', warehouseId); // Debug
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
    const formData = { ...this.transactionForm.value };
    formData.warehouseId = parseInt(formData.warehouseId);
    formData.supplierId = parseInt(formData.supplierId);
    formData.variantId = parseInt(formData.variantId);
    formData.transactionDate = formData.transactionDate ? formData.transactionDate : this.dateUtility.getTodayInIST();
    formData.filledReceived = parseInt(formData.filledReceived);
    formData.emptySent = parseInt(formData.emptySent);

    // Prevent both fields being zero
    if (formData.filledReceived === 0 && formData.emptySent === 0) {
      this.toastr.error('At least one of Filled Received or Empty Sent must be greater than zero.', 'Validation Error');
      return;
    }

    // Prevent sending more empty than available in inventory (warehouse-specific)
    if (formData.emptySent > 0) {
      this.inventoryStockService.getStockByWarehouse(formData.warehouseId).subscribe(stocks => {
        const stockForVariant = (stocks || []).find((s: any) => s.variantId === formData.variantId);
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
      }, err => {
        const errorMessage = err?.error?.message || err?.message || 'Could not validate inventory. Please try again.';
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
      next: (stocks: any[]) => {
        const stockForVariant = (stocks || []).find((s: any) => s.variantId === Number(variantId));
        const currentEmpty = stockForVariant?.emptyQty ?? 0;
        const originalEmptySent = this.editingId ? (this.originalTransaction?.emptySent ?? 0) : 0;
        // Show available empties (include original sent when editing)
        this.currentEmpty = currentEmpty + originalEmptySent;
        this.emptyLoading = false;
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        console.error('Error loading current empty stock:', error);
        this.emptyError = 'Current empty unavailable';
        this.currentEmpty = null;
        this.emptyLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  _submitTransaction(formData: any) {
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
      const id = typeof this.editingId === 'string' ? parseInt(this.editingId, 10) : this.editingId;
      this.transactionService.updateTransaction(id, formData).subscribe({
        next: () => {
          this.toastr.success('Transaction updated successfully', 'Success');
          this.loadTransactions();
          this.closeForm();
        },
        error: (error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error updating transaction';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
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
          console.error('Full error:', error);
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
    this.filteredTransactions = filtered.sort((a: any, b: any) => b.id - a.id);
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
    this.filteredTransactions = [...this.allTransactions].sort((a: any, b: any) => b.id - a.id);
    this.cdr.markForCheck();
  }

  closeForm() {
    this.showForm = false;
    this.editingId = null;
    this.cdr.markForCheck();
  }

  viewTransactionDetails(transaction: any) {
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

  private applyCreatedByFilter(entries: any[]): any[] {
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
