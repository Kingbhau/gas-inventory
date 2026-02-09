import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faEdit, faTrash, faPlus, faTimes, faExclamation, faBuilding } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../services/supplier.service';
import { AuthService } from '../../services/auth.service';
import { LoadingService } from '../../services/loading.service';
import { of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-supplier-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule],
  templateUrl: './supplier-management.component.html',
  styleUrl: './supplier-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierManagementComponent implements OnInit, OnDestroy {
    // Pagination state for suppliers table
    supplierPage = 1;
    supplierPageSize = 10;
    totalSuppliers = 0;
    totalPages = 1;

    get paginatedSuppliers() {
      if (!this.searchTerm || this.searchTerm.trim() === '') {
        return this.suppliers;
      }
      const term = this.searchTerm.trim().toLowerCase();
      return this.suppliers.filter(s =>
        (s.name && s.name.toLowerCase().includes(term)) ||
        (s.contact && s.contact.toLowerCase().includes(term))
      );
    }
    getTotalPages() {
      return this.totalPages;
    }
  supplierForm!: FormGroup;
  showForm = false;
  editingId: number | null = null;
  searchTerm = '';

  // Font Awesome Icons
  faSearch = faSearch;
  faEdit = faEdit;
  faTrash = faTrash;
  faPlus = faPlus;
  faTimes = faTimes;
  faExclamation = faExclamation;
  faBuilding = faBuilding;
  suppliers: any[] = [];

  constructor(
    private fb: FormBuilder,
    private supplierService: SupplierService,
    private authService: AuthService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadSuppliers();
  }

  ngOnDestroy() {}

  loadSuppliers() {
    this.loadingService.show('Loading suppliers...');
    const sub = this.supplierService.getAllSuppliers(this.supplierPage - 1, this.supplierPageSize)
      .pipe(
        catchError((error: any) => {
          const errorMessage = error?.error?.message || error?.message || 'Error loading suppliers';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
          return of({ content: [], totalElements: 0, totalPages: 1 });
        }),
        finalize(() => this.loadingService.hide())
      )
      .subscribe((data: any) => {
        this.suppliers = (data.content || data).sort((a: any, b: any) => b.id - a.id);
        this.totalSuppliers = data.totalElements || this.suppliers.length;
        this.totalPages = data.totalPages || 1;
        this.cdr.markForCheck();
        sub.unsubscribe();
      });
  }

  onPageChange(page: number) {
    this.supplierPage = page;
    this.loadSuppliers();
  }

  onPageSizeChange(size: number) {
    this.supplierPageSize = size;
    this.supplierPage = 1;
    this.loadSuppliers();
  }

  // Remove filteredSuppliers for backend pagination

  initForm() {
    this.supplierForm = this.fb.group({
      code: [''],
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      contact: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(20), Validators.pattern('^[+0-9\-\s()]+$')]]
    });
  }

  openAddForm() {
    this.showForm = true;
    this.editingId = null;
    this.supplierForm.reset();
  }

  closeForm() {
    this.showForm = false;
    this.supplierForm.reset();
  }

  editSupplier(supplier: any) {
    this.showForm = true;
    this.editingId = supplier.id;
    this.supplierForm.patchValue(supplier);
  }

  saveSupplier() {
    if (!this.supplierForm.valid) {
      this.supplierForm.markAllAsTouched();
      this.toastr.error('Please correct the errors in the form.', 'Validation Error');
      return;
    }
    // Prevent duplicate supplier name on frontend
    const name = this.supplierForm.get('name')?.value.trim().toLowerCase();
    const duplicate = this.suppliers.some(s => s.name.trim().toLowerCase() === name && s.id !== this.editingId);
    if (duplicate) {
      this.toastr.error('A supplier with this name already exists.', 'Validation Error');
      return;
    }
    if (this.editingId) {
      const sub = this.supplierService.updateSupplier(this.editingId, this.supplierForm.value)
        .pipe(
          catchError((error: any) => {
            const errorMessage = error?.error?.message || error?.message || 'Error updating supplier';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
            return of(null);
          })
        )
        .subscribe(updatedSupplier => {
          if (updatedSupplier) {
            const index = this.suppliers.findIndex(s => s.id === this.editingId);
            if (index > -1) {
              this.suppliers[index] = updatedSupplier;
            }
            this.supplierService.invalidateCache();
            this.toastr.success('Supplier updated successfully', 'Success');
            this.showForm = false;
            this.supplierForm.reset();
            this.cdr.markForCheck();
          }
          sub.unsubscribe();
        });
    } else {
      const userInfo = this.authService.getUserInfo();
      const businessId = userInfo && userInfo.businessId ? userInfo.businessId : null;
      
      if (!businessId) {
        this.toastr.error('User business information not found', 'Error');
        return;
      }

      const sub = this.supplierService.createSupplier(this.supplierForm.value, businessId)
        .pipe(
          catchError((error: any) => {
            const errorMessage = error?.error?.message || error?.message || 'Error creating supplier';
            this.toastr.error(errorMessage, 'Error');
            console.error('Full error:', error);
            return of(null);
          })
        )
        .subscribe(newSupplier => {
          if (newSupplier) {
            this.suppliers.push(newSupplier);
            this.supplierService.invalidateCache();
            this.toastr.success('Supplier created successfully', 'Success');
            this.showForm = false;
            this.supplierForm.reset();
            this.cdr.markForCheck();
          }
          sub.unsubscribe();
        });
    }
  }

  deleteSupplier(id: number) {
    if (confirm('Are you sure you want to delete this supplier?')) {
      this.supplierService.deleteSupplier(id).subscribe({
        next: () => {
          this.suppliers = this.suppliers.filter(s => s.id !== id);
          this.supplierService.invalidateCache();
          this.toastr.success('Supplier deleted successfully', 'Success');
        },
        error: (error) => {
          const errorMessage = error?.error?.message || error?.message || 'Error deleting supplier';
          this.toastr.error(errorMessage, 'Error');
          console.error('Full error:', error);
        }
      });
    }
  }
}
