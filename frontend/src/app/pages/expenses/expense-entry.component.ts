import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChildren, QueryList } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { SharedModule } from '../../shared/shared.module';
import { ExpenseService } from '../../services/expense.service';
import { PaymentModeService } from '../../services/payment-mode.service';
import { BankAccountService } from '../../services/bank-account.service';
import { Expense } from '../../models/expense.model';
import { ExpenseCategory } from '../../models/expense-category.model';
import { PaymentMode } from '../../models/payment-mode.model';
import { BankAccount } from '../../models/bank-account.model';
import { LoadingService } from '../../services/loading.service';
import { DataRefreshService } from '../../services/data-refresh.service';
import { DateUtilityService } from '../../services/date-utility.service';
import { AuthService } from '../../services/auth.service';
import { finalize, forkJoin, Subject, takeUntil } from 'rxjs';
import { AutocompleteInputComponent } from '../../shared/components/autocomplete-input.component';

@Component({
  selector: 'app-expense-entry',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule, AutocompleteInputComponent],
  templateUrl: './expense-entry.component.html',
  styleUrl: './expense-entry.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseEntryComponent implements OnInit, OnDestroy {
  @ViewChildren(AutocompleteInputComponent) autocompleteInputs!: QueryList<AutocompleteInputComponent>;
  
  expenseForm!: FormGroup;
  successMessage = '';
  categories: ExpenseCategory[] = [];
  categoryMap: Map<string, number> = new Map();
  bankAccounts: BankAccount[] = [];
  paymentModes: PaymentMode[] = [];
  isSubmitting = false;
  isStaff = false;
  isManager = false;
  minAllowedManagerDate = '';
  maxAllowedEntryDate = '';
  private destroy$ = new Subject<void>();
  showDuplicateConfirm = false;
  duplicateExpenseInfo: {
    description: string;
    categoryName: string;
    amount: number;
    expenseDate: string;
  } | null = null;
  pendingExpense: Expense | null = null;

  // Font Awesome Icons
  faCheckCircle = faCheckCircle;

  constructor(
    private fb: FormBuilder,
    private expenseService: ExpenseService,
    private paymentModeService: PaymentModeService,
    private bankAccountService: BankAccountService,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private dataRefreshService: DataRefreshService,
    private cdr: ChangeDetectorRef,
    private dateUtility: DateUtilityService,
    private authService: AuthService
  ) {
    this.initForm();
  }

  ngOnInit() {
    const role = this.authService.getUserInfo()?.role || '';
    this.isStaff = role === 'STAFF';
    this.isManager = role === 'MANAGER';
    this.initializeDateRestrictionBounds();
    this.applyStaffDateRestriction();
    this.loadCategoriesAndPaymentData();
    // Set today's date as default (in Indian timezone)
    const today = this.dateUtility.getTodayInIST();
    this.expenseForm.patchValue({
      expenseDate: today
    });
  }

  private initializeDateRestrictionBounds() {
    const today = this.dateUtility.getTodayInIST();
    this.maxAllowedEntryDate = today;
    const yesterday = this.dateUtility.addDays(new Date(`${today}T00:00:00`), -1);
    this.minAllowedManagerDate = this.dateUtility.getLocalDateString(yesterday);
  }

  private applyStaffDateRestriction() {
    if (!this.isStaff) {
      return;
    }
    const today = this.dateUtility.getTodayInIST();
    this.expenseForm.get('expenseDate')?.setValue(today, { emitEvent: false });
    this.expenseForm.get('expenseDate')?.disable({ emitEvent: false });
  }

  private loadCategoriesAndPaymentData() {
    this.loadingService.show('Loading expense data...');
    forkJoin([
      this.expenseService.getCategories(),
      this.paymentModeService.getActivePaymentModes(),
      this.bankAccountService.getActiveBankAccounts()
    ])
    .pipe(
      finalize(() => this.loadingService.hide()),
      takeUntil(this.destroy$)
    )
    .subscribe({
      next: ([categoryResponse, paymentModes, bankAccounts]: [ExpenseCategory[], PaymentMode[], BankAccount[]]) => {
        const categoryList = Array.isArray(categoryResponse) ? categoryResponse : [];
        this.categories = categoryList.filter((cat) => cat.isActive !== false);
        this.categories.forEach((cat) => {
          if (typeof cat.id === 'number') {
            this.categoryMap.set(cat.name, cat.id);
          }
        });
        this.paymentModes = paymentModes || [];
        this.bankAccounts = bankAccounts || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.toastr.error('Failed to load expense data');
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

  initForm() {
    this.expenseForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(3)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      category: ['', Validators.required],
      expenseDate: ['', Validators.required],
      notes: [''],
      paymentMode: ['', Validators.required],
      bankAccountId: [null]
    });

    // When paymentMode changes, show/hide bank account field
    this.expenseForm.get('paymentMode')?.valueChanges.subscribe((mode) => {
      const bankAccountControl = this.expenseForm.get('bankAccountId');
      const selectedMode = this.getSelectedPaymentMode(mode);
      
      if (selectedMode && selectedMode.isBankAccountRequired) {
        bankAccountControl?.setValidators(Validators.required);
      } else {
        bankAccountControl?.clearValidators();
        bankAccountControl?.reset();
      }
      bankAccountControl?.updateValueAndValidity();
    });
  }

  getSelectedPaymentMode(modeName: string): PaymentMode | undefined {
    return this.paymentModes.find(mode => mode.name === modeName);
  }


  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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
      amount: this.parseAmountInput(formValue.amount),
      categoryId: categoryId
    };

    this.checkDuplicateAndSubmit(expense);
  }

  private normalizeDescription(value: string | null | undefined): string {
    return (value || '')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase();
  }

  private parseAmountInput(value: unknown): number {
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      const cleaned = value.replace(/,/g, '').trim();
      const parsed = parseFloat(cleaned);
      return isNaN(parsed) ? 0 : parsed;
    }
    return 0;
  }

  private normalizeAmount(value: number | string | null | undefined): number {
    const parsed = this.parseAmountInput(value);
    return Math.round(parsed * 100) / 100;
  }

  private getLatestExpense() {
    return this.expenseService.getAllExpenses(0, 1).pipe(takeUntil(this.destroy$));
  }

  private isDuplicateExpense(lastExpense: Expense | null, expense: Expense): boolean {
    if (!lastExpense) return false;
    const sameDescription = this.normalizeDescription(lastExpense.description) === this.normalizeDescription(expense.description);
    const sameCategory = lastExpense.categoryId === expense.categoryId;
    const sameAmount = this.normalizeAmount(lastExpense.amount) === this.normalizeAmount(expense.amount);
    const sameDate = String(lastExpense.expenseDate || '') === String(expense.expenseDate || '');
    return sameDescription && sameCategory && sameAmount && sameDate;
  }

  private buildDuplicateExpenseInfo(expense: Expense) {
    return {
      description: expense.description,
      categoryName: this.expenseForm.get('category')?.value || 'Category',
      amount: this.normalizeAmount(expense.amount),
      expenseDate: String(expense.expenseDate || '')
    };
  }

  private checkDuplicateAndSubmit(expense: Expense): void {
    this.getLatestExpense()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const lastExpense = response?.items?.length ? response.items[0] : null;
          if (lastExpense && this.isDuplicateExpense(lastExpense, expense)) {
            this.pendingExpense = expense;
            this.duplicateExpenseInfo = this.buildDuplicateExpenseInfo(expense);
            this.showDuplicateConfirm = true;
            this.isSubmitting = false;
            this.cdr.markForCheck();
            return;
          }
          this.createExpense(expense);
        },
        error: () => {
          this.createExpense(expense);
        }
      });
  }

  confirmDuplicateExpense(): void {
    if (!this.pendingExpense) {
      this.showDuplicateConfirm = false;
      this.duplicateExpenseInfo = null;
      return;
    }
    const expense = this.pendingExpense;
    this.pendingExpense = null;
    this.showDuplicateConfirm = false;
    this.duplicateExpenseInfo = null;
    this.createExpense(expense);
  }

  cancelDuplicateExpense(): void {
    this.pendingExpense = null;
    this.showDuplicateConfirm = false;
    this.duplicateExpenseInfo = null;
    this.isSubmitting = false;
    this.cdr.markForCheck();
  }

  private createExpense(expense: Expense): void {
    this.expenseService.createExpense(expense)
      .pipe(
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          this.toastr.success('Expense recorded successfully');
          
          // Notify dashboard of the change
          this.dataRefreshService.notifyExpenseCreated(response);
          
          // Reset autocomplete inputs
          if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
            this.autocompleteInputs.forEach(input => {
              if (input.resetInput) {
                input.resetInput();
              }
            });
          }
          
          // Reset form completely
          this.expenseForm.reset();
          
          // Set default values
          const today = this.dateUtility.getTodayInIST();
          this.expenseForm.patchValue({
            description: '',
            category: '',
            amount: '',
            expenseDate: today,
            notes: '',
            paymentMode: '',
            bankAccountId: null
          });
          
          // Mark form as pristine and untouched
          this.expenseForm.markAsPristine();
          this.expenseForm.markAsUntouched();
          this.applyStaffDateRestriction();

          // Clear success message after 3 seconds
          setTimeout(() => {
            this.successMessage = '';
            this.cdr.markForCheck();
          }, 3000);
        },
        error: (error) => {
          this.toastr.error(error?.error?.message || 'Failed to record expense');
        }
      });
  }

  resetForm() {
    this.isSubmitting = false;
    
    // Reset autocomplete inputs first
    if (this.autocompleteInputs && this.autocompleteInputs.length > 0) {
      this.autocompleteInputs.forEach(input => {
        if (input.resetInput) {
          input.resetInput();
        }
      });
    }
    
    // Reset form
    this.expenseForm.reset();
    
    const today = this.dateUtility.getTodayInIST();
    this.expenseForm.patchValue({
      description: '',
      category: '',
      amount: '',
      expenseDate: today,
      notes: '',
      paymentMode: '',
      bankAccountId: null
    });
    
    // Mark form as pristine and untouched
    this.expenseForm.markAsPristine();
    this.expenseForm.markAsUntouched();
    this.applyStaffDateRestriction();
    
    this.successMessage = '';
    this.cdr.markForCheck();
  }
}
