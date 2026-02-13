import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AlertService } from '../../../services/alert.service';
import { Alert } from '../../../models/alert.model';
import { AuthService } from '../../../services/auth.service';
import { Subscription, of } from 'rxjs';

@Component({
  selector: 'app-navbar-alerts',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar-alerts.component.html',
  styleUrl: './navbar-alerts.component.css'
})
export class NavbarAlertsComponent implements OnInit, OnDestroy {
  alerts$;
  alertCount$;
  showDropdown = false;
  private subscriptions: Subscription[] = [];

  constructor(private alertService: AlertService, private authService: AuthService) {
  }

  ngOnInit(): void {
    // Only initialize alerts if user is logged in
    const userInfo = this.authService.getUserInfo();
    if (userInfo) {
      this.alertService.initialize();
    } else {
    }
    
    try {
      this.alerts$ = this.alertService.getAlerts$();
      this.alertCount$ = this.alertService.getAlertCount$();
    } catch (e) {
      this.alerts$ = of([]);
      this.alertCount$ = of(0);
    }
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
  }

  closeDropdown(): void {
    this.showDropdown = false;
  }

  dismissAlert(alertId: number, event: Event): void {
    event.stopPropagation();
    this.alertService.dismissAlert(alertId).subscribe(
      () => {
      },
      (error: any) => {
      }
    );
  }

  formatTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
