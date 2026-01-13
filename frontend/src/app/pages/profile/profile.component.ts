import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUserCircle, faEdit, faKey } from '@fortawesome/free-solid-svg-icons';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { LoadingService } from '../../services/loading.service';
import { finalize } from 'rxjs';

@Component({
    selector: 'app-profile',
    standalone: true,
    imports: [CommonModule, FontAwesomeModule, FormsModule, ReactiveFormsModule],
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {

    faUserCircle = faUserCircle;
    faEdit = faEdit;
    faKey = faKey;
    profileForm!: FormGroup;
    passwordForm!: FormGroup;
    user: any;
    showEdit = false;
    showPassword = false;

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private authService: AuthService,
        private toastr: ToastrService,
        private loadingService: LoadingService
    ) { }

    ngOnInit(): void {
        const localUser = this.authService.getUserInfo();
        if (localUser && localUser.id) {
            this.loadingService.show('Loading profile...');
            this.userService.getUser(localUser.id)
                .pipe(finalize(() => this.loadingService.hide()))
                .subscribe({
                    next: (user) => {
                        this.user = user;
                        this.initForms();
                    },
                    error: () => {
                        // fallback to local user if backend fails
                        this.user = localUser;
                        this.initForms();
                    }
                });
        } else {
            this.user = localUser;
            this.initForms();
        }
    }
    openEditModal() {
        this.showEdit = true;
    }
    closeEditModal() {
        this.showEdit = false;
    }
    openPasswordModal() {
        this.showPassword = true;
    }
    closePasswordModal() {
        this.showPassword = false;
    }
    private initForms() {
        this.profileForm = this.fb.group({
            username: [{ value: this.user?.username || '', disabled: true }],
            name: [this.user?.name || '', Validators.required],
            mobileNo: [this.user?.mobileNo || '', [Validators.required, Validators.pattern('^[6-9][0-9]{9}$')]],
            role: [{ value: this.user?.role || '', disabled: true }]
        });
        this.passwordForm = this.fb.group({
            currentPassword: ['', Validators.required],
            newPassword: ['', [Validators.required, Validators.minLength(6)]],
            confirmPassword: ['', Validators.required]
        });
    }

    updateProfile() {
        if (this.profileForm.valid) {
            const updated = {
                ...this.user,
                name: this.profileForm.get('name')?.value,
                mobileNo: this.profileForm.get('mobileNo')?.value
            };
            this.userService.updateUser(this.user.id, updated).subscribe({
                next: (result) => {
                    // Only update user info (no token)
                    this.authService.setUserInfo(result);
                    this.user = result;
                    this.showEdit = false;
                    this.toastr.success('Profile updated successfully.', 'Success');
                },
                error: () => {
                    this.toastr.error('Failed to update profile.', 'Error');
                }
            });
        }
    }

    resetPassword() {
        if (this.passwordForm.valid && this.passwordForm.get('newPassword')?.value === this.passwordForm.get('confirmPassword')?.value) {
            const currentPassword = this.passwordForm.get('currentPassword')?.value;
            const newPassword = this.passwordForm.get('newPassword')?.value;
            this.userService.changePassword(this.user.id, currentPassword, newPassword).subscribe({
                next: (success) => {
                    if (success) {
                        this.toastr.success('Password changed successfully.', 'Success');
                        this.showPassword = false;
                        this.passwordForm.reset();
                    } else {
                        this.toastr.error('Current password is incorrect.', 'Error');
                    }
                },
                error: () => {
                    this.toastr.error('Failed to change password.', 'Error');
                }
            });
        } else {
            this.toastr.error('Passwords do not match or are invalid.', 'Error');
        }
    }
}
