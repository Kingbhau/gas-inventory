
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSearch, faPencil, faTrash, faPlus, faTimes, faEllipsisV, faExclamation, faUsers } from '@fortawesome/free-solid-svg-icons';
import { UserService, User } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { faEye } from '@fortawesome/free-solid-svg-icons';
import { LoadingService } from '../../services/loading.service';
import { finalize, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
// import your UserService here
// import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, FontAwesomeModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManagementComponent implements OnInit, OnDestroy {
  userForm!: FormGroup;
  showForm = false;
  editingId: number | null = null;
  showDetailsModal = false;
  detailsUser: any = null;
  searchTerm = '';
  isSubmitting = false;
  private destroy$ = new Subject<void>();

  faSearch = faSearch;
  faPencil = faPencil;
  faTrash = faTrash;
  faPlus = faPlus;
  faTimes = faTimes;
  faEllipsisV = faEllipsisV;
  faExclamation = faExclamation;
  faUsers = faUsers;
  faEye = faEye;

  users: any[] = [];
  userPage = 1;
  userPageSize = 10;
  totalUsers = 0;
  totalPages = 1;

  showDeleteModal = false;
  deleteUserTarget: any = null;

  currentUserId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private toastr: ToastrService,
    private userService: UserService,
    private authService: AuthService,
    private loadingService: LoadingService
  ) {
    const userInfo = this.authService.getUserInfo();
    this.currentUserId = userInfo && userInfo.id ? userInfo.id : null;
  }

  ngOnInit(): void {
    const userInfo = this.authService.getUserInfo();
    const businessId = userInfo && userInfo.businessId ? userInfo.businessId : null;
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      mobileNo: ['', [Validators.required, Validators.pattern('^[6-9][0-9]{9}$')]],
      role: ['', Validators.required],
      active: [true, Validators.required],
      businessId: [businessId]
    });
    this.loadUsers();
    this.updatePagination();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openDeleteModal(user: any) {
    this.deleteUserTarget = user;
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
    this.deleteUserTarget = null;
    this.cdr.markForCheck();
  }

  confirmDeleteUser() {
    if (!this.deleteUserTarget) return;
    const id = this.deleteUserTarget.id;
    this.userService.deleteUser(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastr.success('User deleted successfully.', 'Success');
          this.loadUsers();
          this.closeDeleteModal();
        },
        error: () => {
          this.toastr.error('Failed to delete user.', 'Error');
          this.closeDeleteModal();
        }
      });
  }

  loadUsers() {
    this.loadingService.show('Loading users...');
    this.userService.getUsers()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingService.hide())
      )
      .subscribe({
        next: (users) => {
          this.users = users;
          this.updatePagination();
          this.cdr.markForCheck();
        },
        error: () => {
          this.toastr.error('Failed to load users.', 'Error');
          this.cdr.markForCheck();
        }
      });
  }

  get paginatedUsers() {
    let filtered = this.users;
    // Exclude current user
    if (this.currentUserId !== null) {
      filtered = filtered.filter(u => u.id !== this.currentUserId);
    }
    if (this.searchTerm && this.searchTerm.trim() !== '') {
      const term = this.searchTerm.trim().toLowerCase();
      filtered = filtered.filter(u =>
        (u.name && u.name.toLowerCase().includes(term)) ||
        (u.mobile && u.mobile.toLowerCase().includes(term)) ||
        (u.role && u.role.toLowerCase().includes(term))
      );
    }
    this.totalUsers = filtered.length;
    this.totalPages = Math.max(1, Math.ceil(this.totalUsers / this.userPageSize));
    const start = (this.userPage - 1) * this.userPageSize;
    return filtered.slice(start, start + this.userPageSize);
  }

  getTotalPages() {
    return this.totalPages;
  }

  onPageChange(page: number) {
    if (page < 1 || page > this.getTotalPages()) return;
    this.userPage = page;
    this.cdr.markForCheck();
  }

  onPageSizeChange(size: number) {
    this.userPageSize = size;
    this.userPage = 1;
    this.updatePagination();
    this.cdr.markForCheck();
  }

  updatePagination() {
    this.totalUsers = this.users.length;
    this.totalPages = Math.max(1, Math.ceil(this.totalUsers / this.userPageSize));
    if (this.userPage > this.totalPages) this.userPage = this.totalPages;
  }

  isLastRow(user: any): boolean {
    const index = this.paginatedUsers.indexOf(user);
    return index === this.paginatedUsers.length - 1;
  }

  get isAnyDropdownOpen(): boolean {
    return this.paginatedUsers && Array.isArray(this.paginatedUsers)
      ? this.paginatedUsers.some((u: any) => u && u.showMenu)
      : false;
  }

  openAddForm() {
    this.showForm = true;
    this.editingId = null;
    this.userForm.reset();
    const userInfo = this.authService.getUserInfo();
    const businessId = userInfo && userInfo.businessId ? userInfo.businessId : null;
    this.userForm.patchValue({ active: true, businessId });
    this.cdr.markForCheck();
  }

  editUser(user: any) {
    this.showForm = true;
    this.editingId = user.id;
    this.userForm.patchValue({
      ...user,
      active: user.active !== undefined ? user.active : true
    });
    this.cdr.markForCheck();
  }

  openDetailsModal(user: any) {
    this.detailsUser = user;
    this.showDetailsModal = true;
    this.cdr.markForCheck();
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.detailsUser = null;
    this.cdr.markForCheck();
  }

  saveUser() {
    if (!this.userForm.valid) {
      this.userForm.markAllAsTouched();
      this.toastr.error('Please correct the errors in the form.', 'Validation Error');
      return;
    }

    // Prevent duplicate submissions
    if (this.isSubmitting) {
      return;
    }

    const mobileNo = this.userForm.get('mobileNo')?.value;
    const duplicate = this.users.some(u => u.mobileNo === mobileNo && u.id !== this.editingId);
    if (duplicate) {
      this.toastr.error('A user with this mobile number already exists.', 'Validation Error');
      return;
    }

    this.isSubmitting = true;
    this.cdr.markForCheck();

    if (this.editingId) {
      this.userService.updateUser(this.editingId, this.userForm.value)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (result) => {
            this.toastr.success('User updated successfully.', 'Success');
            this.loadUsers();
            // Update localStorage if the edited user is the current user
            const currentUser = JSON.parse(localStorage.getItem('user_info') || '{}');
            if (currentUser && currentUser.id === result.id) {
              // Only update user info (no token)
              const userInfo = {
                id: result.id,
                name: result.name,
                role: result.role,
                mobileNo: result.mobileNo || '',
                businessId: result.businessId || (currentUser && currentUser.businessId) || null
              };
              localStorage.setItem('user_info', JSON.stringify(userInfo));
              if (result.name) localStorage.setItem('user_name', result.name);
              if (result.role) localStorage.setItem('user_role', result.role);
            }
            this.closeForm();
            this.isSubmitting = false;
            this.cdr.markForCheck();
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to update user.';
            this.toastr.error(msg, 'Error');
            this.isSubmitting = false;
            this.cdr.markForCheck();
          }
        });
    } else {
      const userInfo = this.authService.getUserInfo();
      const businessId = userInfo && userInfo.businessId ? userInfo.businessId : null;
      const userPayload = { ...this.userForm.value, businessId };
      this.userService.addUser(userPayload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.toastr.success('User added successfully.', 'Success');
            this.loadUsers();
            this.closeForm();
            this.isSubmitting = false;
            this.cdr.markForCheck();
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to add user.';
            this.toastr.error(msg, 'Error');
            this.isSubmitting = false;
            this.cdr.markForCheck();
            // Do not close the form on error
          }
        });
    }
  }
  deleteUser(id: number) {
    const user = this.users.find(u => u.id === id);
    if (!user) return;
    if (confirm(`Are you sure you want to delete user "${user.name}"?`)) {
      this.userService.deleteUser(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.toastr.success('User deleted successfully.', 'Success');
            this.loadUsers();
          },
          error: () => {
            this.toastr.error('Failed to delete user.', 'Error');
          }
        });
    }
  }

  closeForm() {
    this.showForm = false;
    this.editingId = null;
    this.isSubmitting = false;
    this.userForm.reset();
    this.userForm.patchValue({ active: true });
    this.cdr.markForCheck();
  }
}
