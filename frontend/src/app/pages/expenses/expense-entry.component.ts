import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { ExpenseService } from '../../services/expense.service';
import { Expense } from '../../models/expense.model';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-expense-entry',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './expense-entry.component.html',
  styleUrl: './expense-entry.component.css'
})
export class ExpenseEntryComponent implements OnInit {
  expenseForm!: FormGroup;
  successMessage = '';
  categories: any[] = [];
  categoryMap: Map<string, number> = new Map();
  isSubmitting = false;

  // Font Awesome Icons
  faCheckCircle = faCheckCircle;

  constructor(
    private fb: FormBuilder,
    private expenseService: ExpenseService,
    private toastr: ToastrService,
    private loadingService: LoadingService
  ) {
    this.initForm();
  }

  ngOnInit() {
    this.loadCategories();
    // Set today's date as default
    const today = new Date().toISOString().split('T')[0];
    this.expenseForm.patchValue({
      expenseDate: today
    });
  }

  initForm() {
    this.expenseForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(3)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      category: ['', Validators.required],
      expenseDate: ['', Validators.required],
      notes: ['']
    });
  }

  loadCategories() {
    this.loadingService.show('Loading categories...');
    this.expenseService.getCategories()
      .pipe(finalize(() => this.loadingService.hide()))
      .subscribe({
        next: (response: any) => {
          const categoryList = Array.isArray(response) ? response : (response.content || []);
          this.categories = categoryList;
          // Create a map of category names to IDs
          categoryList.forEach((cat: any) => {
            this.categoryMap.set(cat.name, cat.id);
          });
        },
        error: () => {
          this.toastr.error('Failed to load categories');
          // Fallback categories
          const fallbackCategories = [
            { id: 1, name: 'Salary' },
            { id: 2, name: 'Utilities' },
            { id: 3, name: 'Maintenance' },
            { id: 4, name: 'Transportation' },
            { id: 5, name: 'Office Supplies' },
            { id: 6, name: 'Other' }
          ];
          this.categories = fallbackCategories;
          fallbackCategories.forEach((cat) => {
            this.categoryMap.set(cat.name, cat.id);
        });
      }
    });
  }

  onSubmit() {
    if (this.expenseForm.invalid) {
      this.toastr.error('Please fill all required fields correctly');
      Object.keys(this.expenseForm.controls).forEach(key => {
        this.expenseForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    const formValue = this.expenseForm.value;
    
    // Map category name to ID
    const categoryId = this.categoryMap.get(formValue.category);
    if (!categoryId) {
      this.toastr.error('Invalid category selected');
      this.isSubmitting = false;
      return;
    }

    const expense: Expense = {
      ...formValue,
      categoryId: categoryId
    };

    this.expenseService.createExpense(expense).subscribe({
      next: (response) => {
        this.successMessage = 'Expense recorded successfully!';
        this.toastr.success('Expense recorded successfully');
        this.expenseForm.reset();
        
        // Set today's date as default for next entry
        const today = new Date().toISOString().split('T')[0];
        this.expenseForm.patchValue({
          expenseDate: today
        });

        // Clear success message after 3 seconds
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);

        this.isSubmitting = false;
      },
      error: (error) => {
        this.toastr.error(error?.error?.message || 'Failed to record expense');
        this.isSubmitting = false;
      }
    });
  }

  resetForm() {
    this.expenseForm.reset();
    const today = new Date().toISOString().split('T')[0];
    this.expenseForm.patchValue({
      expenseDate: today
    });
    this.successMessage = '';
  }
}
