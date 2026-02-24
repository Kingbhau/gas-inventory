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
import { finalize, Subject, debounceTime, distinctUntilChanged, takeUntil, catchError, of } from 'rxjs';
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
import { SupplierBorrowBalance } from '../../models/supplier-borrow-balance.model';

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
  selectedTransactionTypeTab: string = 'ALL';
  selectedTransaction: SupplierTransaction | null = null;
  originalTransaction: SupplierTransaction | null = null;
  transactionForm!: FormGroup;
  currentEmpty: number | null = null;
  emptyLoading = false;
  emptyError = '';
  borrowBalance: SupplierBorrowBalance | null = null;
  borrowBalanceLoading = false;
  borrowBalanceError = '';

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
  transactionTypes = [
    { value: 'PURCHASE', label: 'Purchase' },
    { value: 'BORROW_IN', label: 'Borrow In' },
    { value: 'BORROW_OUT', label: 'Borrow Out' },
    { value: 'PURCHASE_RETURN', label: 'Purchase Return (Defective)' }
  ];
  transactionTypeTabs = [
    { value: 'ALL', label: 'All' },
    { value: 'PURCHASE', label: 'Purchase' },
    { value: 'BORROW_IN', label: 'Borrow In' },
    { value: 'BORROW_OUT', label: 'Borrow Out' },
    { value: 'PURCHASE_RETURN', label: 'Purchase Return' }
  ];

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
    this.warehouseService.getActiveWarehouses()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: Warehouse[]) => {
          this.warehousesActive = data || [];
          // Keep filter usable even if all-warehouses endpoint fails on first load
          if (!this.warehouses || this.warehouses.length === 0) {
            this.warehouses = [...this.warehousesActive];
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.warehousesActive = [];
          this.warehouses = [];
        }
      });

    this.warehouseService.getAllWarehouses()
      .pipe(
        catchError(() => of([] as Warehouse[]))
      )
      .subscribe({
        next: (data: Warehouse[]) => {
          if (data && data.length > 0) {
            this.warehouses = data;
          }
          this.cdr.markForCheck();
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
    this.transactionService.getAllTransactions(
      this.transactionPage - 1,
      this.transactionPageSize,
      'transactionDate',
      'DESC',
      {
        referenceNumber: this.filterReference || undefined,
        createdBy: this.filterCreatedBy || undefined,
        supplierId: this.selectedSupplier ? Number(this.selectedSupplier) : undefined,
        warehouseId: this.selectedWarehouse ? Number(this.selectedWarehouse) : undefined,
        variantId: this.filterVariantId ? Number(this.filterVariantId) : undefined,
        fromDate: this.filterFromDate || undefined,
        toDate: this.filterToDate || undefined,
        transactionType: this.selectedTransactionTypeTab !== 'ALL' ? this.selectedTransactionTypeTab : undefined
      }
    )
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data: PageResponse<SupplierTransaction>) => {
          this.allTransactions = (data.items || [])
            .filter((t): t is SupplierTransaction & { id: number } => typeof t.id === 'number');
          this.filteredTransactions = [...this.allTransactions];
          this.totalTransactions = data.totalElements ?? this.allTransactions.length;
          this.totalPages = data.totalPages ?? 1;
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
      transactionType: ['PURCHASE', Validators.required],
      transactionDate: ['', Validators.required],
      filledReceived: [null, [Validators.min(0)]],
      emptyReceived: [null, [Validators.min(0)]],
      filledSent: [null, [Validators.min(0)]],
      emptySent: [null, [Validators.min(0)]],
      reference: [''],
      amount: [null, [Validators.required, Validators.min(0)]],
      note: ['']
    });
    this.updateAmountValidators();
    this.updateQuantityValidators();

    this.transactionForm.get('warehouseId')?.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadCurrentEmpty();
        this.loadBorrowBalance();
      });

    this.transactionForm.get('variantId')?.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadCurrentEmpty();
        this.loadBorrowBalance();
      });

    this.transactionForm.get('transactionType')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.updateQuantityValidators();
        this.updateAmountValidators();
        this.loadBorrowBalance();
        this.cdr.markForCheck();
      });

    this.transactionForm.get('supplierId')?.valueChanges
      .pipe(debounceTime(150), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.loadBorrowBalance());
  }

  openAddForm() {
    this.editingId = null;
    this.originalTransaction = null;
    this.transactionForm.reset({
      supplierId: '',
      variantId: '',
      transactionType: 'PURCHASE',
      filledReceived: null,
      emptyReceived: null,
      filledSent: null,
      emptySent: null,
      amount: null,
      note: ''
    });
    this.showForm = true;
    // Refresh variants to get any newly activated ones
    this.variantService.invalidateCache();
    this.loadVariants();
    this.loadCurrentEmpty();
    this.loadBorrowBalance();
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
      transactionType: transaction.transactionType || 'PURCHASE',
      transactionDate: transaction.transactionDate,
      filledReceived: transaction.filledReceived,
      emptyReceived: transaction.emptyReceived ?? 0,
      filledSent: transaction.filledSent ?? 0,
      emptySent: transaction.emptySent,
      reference: transaction.reference,
      amount: transaction.amount || 0,
      note: transaction.note || ''
    });
    this.showForm = true;
    this.loadCurrentEmpty();
    this.loadBorrowBalance();
    this.cdr.markForCheck();
  }

  saveTransaction() {
    if (!this.transactionForm.valid) return;
    const formData: CreateSupplierTransactionRequest = { ...this.transactionForm.value };
    formData.warehouseId = Number(formData.warehouseId);
    formData.supplierId = Number(formData.supplierId);
    formData.variantId = Number(formData.variantId);
    formData.transactionDate = formData.transactionDate ? formData.transactionDate : this.dateUtility.getTodayInIST();
    formData.transactionType = (formData.transactionType || 'PURCHASE') as string;
    formData.filledReceived = Number(formData.filledReceived || 0);
    formData.emptyReceived = Number(formData.emptyReceived || 0);
    formData.filledSent = Number(formData.filledSent || 0);
    formData.emptySent = Number(formData.emptySent || 0);
    formData.amount = this.parseAmountInput(formData.amount);

    if (!this.validateQuantities(formData)) {
      return;
    }

    if (formData.transactionType === 'PURCHASE' && (formData.amount === null || typeof formData.amount === 'undefined')) {
      this.toastr.error('Amount is required for purchase transactions.', 'Validation Error');
      return;
    }

    if (this.requiresInventoryCheck(formData)) {
      this.inventoryStockService.getStockByWarehouse(formData.warehouseId).subscribe((stocks: InventoryStock[]) => {
        const stockForVariant = (stocks || []).find((s) => s.variantId === formData.variantId);
        const currentEmpty = stockForVariant?.emptyQty ?? 0;
        const currentFilled = stockForVariant?.filledQty ?? 0;
        const originalEmptySent = this.editingId ? (this.originalTransaction?.emptySent ?? 0) : 0;
        const originalFilledSent = this.editingId ? (this.originalTransaction?.filledSent ?? 0) : 0;
        const availableEmpty = currentEmpty + originalEmptySent;
        const availableFilled = currentFilled + originalFilledSent;

        if ((formData.emptySent || 0) > availableEmpty) {
          this.toastr.error('Cannot send more empty cylinders than available in inventory for this warehouse.', 'Validation Error');
          return;
        }
        if ((formData.filledSent || 0) > availableFilled) {
          this.toastr.error('Cannot send more filled cylinders than available in inventory for this warehouse.', 'Validation Error');
          return;
        }
        this._submitTransaction(formData);
      }, (err: unknown) => {
        const errorObj = err as { error?: { message?: string }; message?: string };
        const errorMessage = errorObj?.error?.message || errorObj?.message || 'Could not validate inventory. Please try again.';
        this.toastr.error(errorMessage, 'Error');
      });
    } else {
      this._submitTransaction(formData);
    }
  }

  private updateAmountValidators(): void {
    const type = this.transactionForm.get('transactionType')?.value || 'PURCHASE';
    const amountControl = this.transactionForm.get('amount');
    if (!amountControl) return;
    if (type === 'PURCHASE') {
      amountControl.setValidators([Validators.required, Validators.min(0)]);
    } else {
      amountControl.setValidators([Validators.min(0)]);
    }
    amountControl.updateValueAndValidity({ emitEvent: false });
  }

  private updateQuantityValidators(): void {
    const type = this.transactionForm.get('transactionType')?.value || 'PURCHASE';
    const filledReceivedControl = this.transactionForm.get('filledReceived');
    const emptyReceivedControl = this.transactionForm.get('emptyReceived');
    const filledSentControl = this.transactionForm.get('filledSent');
    const emptySentControl = this.transactionForm.get('emptySent');

    [filledReceivedControl, emptyReceivedControl, filledSentControl, emptySentControl]
      .forEach((control) => {
        if (!control) return;
        control.setValidators([Validators.min(0)]);
        control.updateValueAndValidity({ emitEvent: false });
      });

    if (type === 'PURCHASE') {
      filledSentControl?.setValue(null, { emitEvent: false });
      emptyReceivedControl?.setValue(null, { emitEvent: false });
    } else if (type === 'BORROW_IN') {
      filledSentControl?.setValue(null, { emitEvent: false });
      emptySentControl?.setValue(null, { emitEvent: false });
    } else if (type === 'BORROW_OUT') {
      filledReceivedControl?.setValue(null, { emitEvent: false });
      emptyReceivedControl?.setValue(null, { emitEvent: false });
    } else if (type === 'PURCHASE_RETURN') {
      filledReceivedControl?.setValue(null, { emitEvent: false });
      emptyReceivedControl?.setValue(null, { emitEvent: false });
      emptySentControl?.setValue(null, { emitEvent: false });
    }
  }

  private parseAmountInput(value: unknown): number | undefined {
    if (value === null || typeof value === 'undefined' || value === '') {
      return undefined;
    }
    if (typeof value === 'number') {
      return isNaN(value) ? undefined : value;
    }
    if (typeof value === 'string') {
      const cleaned = value.replace(/,/g, '').trim();
      const parsed = parseFloat(cleaned);
      return isNaN(parsed) ? undefined : parsed;
    }
    return undefined;
  }

  private validateQuantities(formData: CreateSupplierTransactionRequest): boolean {
    const fr = Number(formData.filledReceived || 0);
    const er = Number(formData.emptyReceived || 0);
    const fs = Number(formData.filledSent || 0);
    const es = Number(formData.emptySent || 0);
    const type = formData.transactionType || 'PURCHASE';

    if (fr === 0 && er === 0 && fs === 0 && es === 0) {
      this.toastr.error('At least one quantity must be greater than zero.', 'Validation Error');
      return false;
    }

    if (type === 'PURCHASE') {
      if (fs > 0 || er > 0) {
        this.toastr.error('Purchase cannot include filled sent or empty received.', 'Validation Error');
        return false;
      }
      if (fr === 0 && es === 0) {
        this.toastr.error('Purchase requires filled received or empty sent.', 'Validation Error');
        return false;
      }
    }

    if (type === 'BORROW_IN') {
      if (fs > 0 || es > 0) {
        this.toastr.error('Borrow in cannot include filled sent or empty sent.', 'Validation Error');
        return false;
      }
      if (fr === 0 && er === 0) {
        this.toastr.error('Borrow in requires filled received or empty received.', 'Validation Error');
        return false;
      }
    }

    if (type === 'BORROW_OUT') {
      if (fr > 0 || er > 0) {
        this.toastr.error('Borrow out cannot include filled received or empty received.', 'Validation Error');
        return false;
      }
      if (fs === 0 && es === 0) {
        this.toastr.error('Borrow out requires filled sent or empty sent.', 'Validation Error');
        return false;
      }
    }

    if (type === 'PURCHASE_RETURN') {
      if (fr > 0 || er > 0 || es > 0) {
        this.toastr.error('Purchase return supports only filled sent quantity.', 'Validation Error');
        return false;
      }
      if (fs === 0) {
        this.toastr.error('Purchase return requires filled sent quantity.', 'Validation Error');
        return false;
      }
    }

    return true;
  }

  private requiresInventoryCheck(formData: CreateSupplierTransactionRequest): boolean {
    const type = formData.transactionType || 'PURCHASE';
    if (type === 'BORROW_IN') {
      return false;
    }
    return (formData.emptySent || 0) > 0 || (formData.filledSent || 0) > 0;
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

  loadBorrowBalance() {
    const type = this.transactionForm.get('transactionType')?.value;
    if (type !== 'BORROW_OUT') {
      this.borrowBalance = null;
      this.borrowBalanceLoading = false;
      this.borrowBalanceError = '';
      return;
    }
    const supplierId = this.transactionForm.get('supplierId')?.value;
    const warehouseId = this.transactionForm.get('warehouseId')?.value;
    const variantId = this.transactionForm.get('variantId')?.value;
    if (!supplierId || !warehouseId || !variantId) {
      this.borrowBalance = null;
      this.borrowBalanceLoading = false;
      this.borrowBalanceError = '';
      return;
    }
    this.borrowBalanceLoading = true;
    this.borrowBalanceError = '';
    const excludeId = this.editingId ?? null;
    this.transactionService.getBorrowBalance(Number(supplierId), Number(warehouseId), Number(variantId), excludeId)
      .pipe(finalize(() => {
        this.borrowBalanceLoading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (data: SupplierBorrowBalance) => {
          this.borrowBalance = data;
        },
        error: (error: unknown) => {
          const err = error as { error?: { message?: string }; message?: string };
          this.borrowBalanceError = err?.error?.message || err?.message || 'Borrow balance unavailable';
          this.borrowBalance = null;
        }
      });
  }

  getDisplayBorrowBalance(): { filled: number; empty: number } | null {
    if (!this.borrowBalance) return null;
    const extraFilled = this.editingId ? Number(this.transactionForm.get('filledSent')?.value || 0) : 0;
    const extraEmpty = this.editingId ? Number(this.transactionForm.get('emptySent')?.value || 0) : 0;
    return {
      filled: (this.borrowBalance.filledAvailable || 0) + extraFilled,
      empty: (this.borrowBalance.emptyAvailable || 0) + extraEmpty
    };
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
    this.transactionPage = 1;
    this.loadTransactions();
  }

  setTransactionTypeTab(tab: string) {
    this.selectedTransactionTypeTab = tab;
    this.transactionPage = 1;
    this.loadTransactions();
  }

  resetFilters() {
    this.selectedSupplier = '';
    this.selectedWarehouse = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterVariantId = '';
    this.filterReference = '';
    this.filterCreatedBy = '';
    this.selectedTransactionTypeTab = 'ALL';
    this.transactionPage = 1;
    this.loadTransactions();
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
