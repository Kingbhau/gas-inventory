import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBars, faTimes, faSignOut } from '@fortawesome/free-solid-svg-icons';
import { AuthService } from '../../services/auth.service';
import { AlertService, Alert } from '../../services/alert.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit, OnDestroy {
  @Input() title: string = 'Gas Agency';
  @Input() links: Array<{ label: string; path: string; icon?: any }> = [];

  faBars = faBars;
  faTimes = faTimes;
  faSignOut = faSignOut;

  userName: string = '';
  showAlertDropdown = false;
  alertCount = 0;
  alerts: Alert[] = [];
  
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit() {
    this.userName = this.authService.getLoggedInUserName();
    
    // Subscribe to alerts from service
    this.alertService.getAlerts$()
      .pipe(takeUntil(this.destroy$))
      .subscribe((alerts) => {
        this.alerts = alerts;
        this.alertCount = alerts.length;
      });
    
    // Subscribe to alert count
    this.alertService.getAlertCount$()
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => {
        this.alertCount = count;
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleAlertDropdown() {
    this.showAlertDropdown = !this.showAlertDropdown;
    console.log('Bell clicked, dropdown:', this.showAlertDropdown);
  }

  closeAlertDropdown() {
    this.showAlertDropdown = false;
  }

  dismissAlert(alertId: number) {
    this.alertService.dismissAlert(alertId);
  }

  logout() {
    this.alertService.reset();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }
}
