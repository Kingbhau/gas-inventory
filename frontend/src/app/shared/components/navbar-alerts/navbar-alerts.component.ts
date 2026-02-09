import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AlertService, Alert } from '../../../services/alert.service';
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
    console.log('🔔 NavbarAlertsComponent constructor called');
  }

  ngOnInit(): void {
    console.log('🔔 NavbarAlertsComponent initialized');
    
    // Only initialize alerts if user is logged in
    const userInfo = this.authService.getUserInfo();
    if (userInfo) {
      console.log('✅ User is logged in, initializing alerts');
      this.alertService.initialize();
    } else {
      console.log('⚠️ User not logged in, skipping alert initialization');
    }
    
    try {
      this.alerts$ = this.alertService.getAlerts$();
      this.alertCount$ = this.alertService.getAlertCount$();
      
      this.alertCount$.subscribe(count => {
        console.log('🔔 Alert count changed:', count);
      });
    } catch (e) {
      console.error('🔔 Error in ngOnInit:', e);
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
        console.log('Alert dismissed:', alertId);
      },
      (error: any) => {
        console.error('Error dismissing alert:', error);
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
