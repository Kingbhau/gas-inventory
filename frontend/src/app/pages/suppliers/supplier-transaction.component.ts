import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPencil, faTrash, faBox, faDownload } from '@fortawesome/free-solid-svg-icons';
import { exportSupplierTransactionsToPDF } from '../reports/export-supplier-transactions.util';
import { BusinessInfoService } from '../../services/business-info.service';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../services/supplier.service';
import { SupplierTransactionService } from '../../services/supplier-transaction.service';
import { InventoryStockService } from '../../services/inventory-stock.service';
import { CylinderVariantService } from '../../services/cylinder-variant.service';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-supplier-transaction',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './supplier-transaction.component.html',
  styleUrl: './supplier-transaction.component.css'
})
export class SupplierTransactionComponent implements OnInit {
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
  filterFromDate: string = '';
  filterToDate: string = '';
  filterVariantId: string = '';
  transactionForm!: FormGroup;

  // Font Awesome Icons
  faPencil = faPencil;
  faTrash = faTrash;
  faBox = faBox;
  faDownload = faDownload;


  suppliers: any[] = [];
  variants: any[] = [];
  allTransactions: any[] = [];
  filteredTransactions: any[] = [];

  constructor(
    private fb: FormBuilder,
    private supplierService: SupplierService,
    private transactionService: SupplierTransactionService,
    private variantService: CylinderVariantService,
    private toastr: ToastrService,
    private inventoryStockService: InventoryStockService,
    private businessInfoService: BusinessInfoService,
    private loadingService: LoadingService
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
    this.loadSuppliers();
    this.loadVariants();
    this.loadTransactions();
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
      variantName
    });
    this.toastr.success('PDF exported!', 'Success');
  }

  loadSuppliers() {
    this.loadingService.show('Loading suppliers...');
    this.supplierService.getAllSuppliers(0, 100)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.suppliers = data.content || data;
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
    this.variantService.getAllVariants(0, 100)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.variants = data.content || data;
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
    this.transactionService.getAllTransactions(this.transactionPage - 1, this.transactionPageSize)
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (data) => {
          this.allTransactions = (data.content || data).sort((a: any, b: any) => b.id - a.id);
          this.filteredTransactions = [...this.allTransactions];
          this.totalTransactions = data.totalElements || this.allTransactions.length;
          this.totalPages = data.totalPages || 1;
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
      supplierId: ['', Validators.required],
      variantId: ['', Validators.required],
      transactionDate: ['', Validators.required],
      filledReceived: [0, [Validators.required, Validators.min(0)]],
      emptySent: [0, [Validators.required, Validators.min(0)]],
      reference: [''],
      amount: [0, [Validators.required, Validators.min(0)]]
    });
  }

  openAddForm() {
    this.editingId = null;
    this.transactionForm.reset();
    this.showForm = true;
  }

  editTransaction(transaction: any) {
    this.editingId = transaction.id;
    this.transactionForm.patchValue({
      supplierId: transaction.supplierId,
      variantId: transaction.variantId,
      transactionDate: transaction.transactionDate,
      filledReceived: transaction.filledReceived,
      emptySent: transaction.emptySent,
      reference: transaction.reference,
      amount: transaction.amount || 0
    });
    this.showForm = true;
  }

  saveTransaction() {
    if (!this.transactionForm.valid) return;
    const formData = { ...this.transactionForm.value };
    formData.supplierId = parseInt(formData.supplierId);
    formData.variantId = parseInt(formData.variantId);
    formData.transactionDate = formData.transactionDate ? formData.transactionDate : new Date().toISOString().split('T')[0];
    formData.filledReceived = parseInt(formData.filledReceived);
    formData.emptySent = parseInt(formData.emptySent);

    // Prevent both fields being zero
    if (formData.filledReceived === 0 && formData.emptySent === 0) {
      this.toastr.error('At least one of Filled Received or Empty Sent must be greater than zero.', 'Validation Error');
      return;
    }

    // Prevent sending more empty than available in inventory
    if (formData.emptySent > 0) {
      this.inventoryStockService.getStockByVariant(formData.variantId).subscribe(stock => {
        if (formData.emptySent > stock.emptyQty) {
          this.toastr.error('Cannot send more empty cylinders than available in inventory for this variant.', 'Validation Error');
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
    this.filteredTransactions = filtered.sort((a: any, b: any) => b.id - a.id);
  }

  resetFilters() {
    this.selectedSupplier = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterVariantId = '';
    this.filteredTransactions = [...this.allTransactions].sort((a: any, b: any) => b.id - a.id);
  }

  closeForm() {
    this.showForm = false;
    this.editingId = null;
  }
}
