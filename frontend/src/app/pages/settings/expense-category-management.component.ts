import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPlus, faEdit, faTrash, faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { ExpenseCategoryService } from '../../services/expense-category.service';
import { ExpenseCategory } from '../../models/expense-category.model';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-expense-category-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FontAwesomeModule, SharedModule],
  templateUrl: './expense-category-management.component.html',
  styleUrl: './expense-category-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseCategoryManagementComponent implements OnInit, OnDestroy {
  categories: ExpenseCategory[] = [];
  categoryForm!: FormGroup;
  isLoading = false;
  isSubmitting = false;
  showModal = false;
  editingId: number | null = null;
  
  // Icons
  faPlus = faPlus;
  faEdit = faEdit;
  faTrash = faTrash;
  faCheck = faCheck;
  faTimes = faTimes;

  constructor(
    private categoryService: ExpenseCategoryService,
    private fb: FormBuilder,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadCategories();
  }

  initForm() {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      isActive: ['true']
    });
  }

  ngOnDestroy() {}

  loadCategories() {
    this.loadingService.show('Loading categories...');
    this.categoryService.getAllCategoriesAll()
      .pipe(finalize(() => {
        this.loadingService.hide();
        this.isLoading = false;
      }))
      .subscribe({
        next: (response) => {
          this.categories = response || [];
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error('Failed to load categories');
          this.cdr.markForCheck();
        }
      });
  }

  onAddNew() {
    this.editingId = null;
    this.categoryForm.reset({ isActive: 'true' });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  onEdit(category: ExpenseCategory) {
    this.showModal = true;
    this.editingId = category.id || null;
    this.categoryForm.patchValue({
      name: category.name,
      description: category.description || '',
      isActive: (category.isActive !== false ? 'true' : 'false')
    });
    this.cdr.markForCheck();
  }

  closeModal() {
    this.showModal = false;
    this.editingId = null;
    this.categoryForm.reset();
    this.cdr.markForCheck();
  }

  onSubmit() {
    if (this.categoryForm.invalid) {
      this.toastr.error('Please fill all required fields');
      return;
    }

    this.isSubmitting = true;
    const formValue = {
      ...this.categoryForm.value,
      isActive: this.categoryForm.value.isActive === 'true' || this.categoryForm.value.isActive === true
    };

    if (this.editingId) {
      // Update existing
      this.categoryService.updateCategory(this.editingId, formValue).subscribe({
        next: () => {
          this.toastr.success('Category updated successfully');
          this.loadCategories();
          this.closeModal();
          this.isSubmitting = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to update category');
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      // Create new
      this.categoryService.createCategory(formValue).subscribe({
        next: () => {
          this.toastr.success('Category created successfully');
          this.loadCategories();
          this.closeModal();
          this.isSubmitting = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to create category');
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  onDelete(id: number | undefined) {
    if (!id) return;
    
    if (confirm('Are you sure you want to delete this category?')) {
      this.categoryService.deleteCategory(id).subscribe({
        next: () => {
          this.toastr.success('Category deleted successfully');
          this.loadCategories();
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to delete category');
          this.cdr.markForCheck();
        }
      });
    }
  }
}
