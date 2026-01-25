import { Component, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AlertService } from '../../services/alert.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUser, faLock } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, NgIf, FontAwesomeModule],
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;
  faUser = faUser;
  faLock = faLock;

  showForgot = false;
  forgotEmail = '';
  forgotLoading = false;
  forgotMsg = '';


  constructor(private authService: AuthService, private alertService: AlertService, private router: Router, private toastr: ToastrService, private cdr: ChangeDetectorRef) {}

  onLogin() {
    this.loading = true;
    this.error = '';
    this.authService.login(this.username, this.password).subscribe({
      next: (res) => {
        // Only store user info (no token)
        this.authService.setUserInfo(res);
        // Initialize alerts after successful login
        this.alertService.initialize();
        this.router.navigate(['/']);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Show the actual error message from backend error response
        const errorMessage = err?.error?.message || err?.error?.error || 'Invalid username or password';
        this.toastr.error(errorMessage, 'Login Failed');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openForgotPassword(event: Event) {
    event.preventDefault();
    this.showForgot = true;
    this.forgotEmail = '';
    this.forgotMsg = '';
  }

  closeForgotPassword() {
    this.showForgot = false;
    this.forgotEmail = '';
    this.forgotMsg = '';
    this.forgotLoading = false;
  }

  onForgotPassword() {
    if (!this.forgotEmail) return;
    this.forgotLoading = true;
    this.forgotMsg = '';
    this.authService.forgotPassword(this.forgotEmail).subscribe({
      next: () => {
        this.forgotMsg = 'Reset link sent to your email.';
        this.forgotLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.forgotMsg = 'Failed to send reset link.';
        this.forgotLoading = false;
        this.cdr.markForCheck();
      }
    });
  }
}
