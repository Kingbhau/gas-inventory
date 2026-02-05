import { Component, OnInit, OnDestroy, Renderer2, ChangeDetectorRef, NgZone, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBars, faTimes, faChartBar, faPlus, faHistory, faUsers, faUser, faBox, faTruck,faRupeeSign, faCog, faSearch, faBell, faChevronDown, faExchange, faFileAlt, faSignOut, faReceipt, faExclamationTriangle, faClock, faCheck, faXmark, faBook } from '@fortawesome/free-solid-svg-icons';
import { EmptyReturnComponent } from './pages/empty-return/empty-return.component';
import { AuthService } from './services/auth.service';
import { LoadingService, LoadingState } from './services/loading.service';
import { LoaderComponent } from './shared/components/loader.component';
import { HttpClient } from '@angular/common/http';
import { filter, takeUntil } from 'rxjs/operators';
import { AlertService } from './services/alert.service';
import { Subject } from 'rxjs';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, LoaderComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit, OnDestroy {
  private globalClickUnlistener: (() => void) | null = null;
  private destroy$ = new Subject<void>();
  loading$ = this.loadingService.loading$;

  constructor(
    private router: Router,
    private authService: AuthService,
    private loadingService: LoadingService,
    private renderer: Renderer2,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private alertService: AlertService
  ) {}

  get isAuthenticated(): boolean {
    // Authenticated if user info exists
    return !!this.authService.getUserInfo();
  }

  get userRole(): string {
    return localStorage.getItem('user_role') || '';
  }

  get userName(): string {
    return this.authService.getLoggedInUserName();
  }

  get userInfo(): any {
    return this.authService.getUserInfo();
  }

  get isManager(): boolean {
    return this.userInfo?.role === 'MANAGER' || this.userRole === 'MANAGER';
  }
  get isStaff(): boolean {
    return this.userInfo?.role === 'STAFF' || this.userRole === 'STAFF';
  }
  get isOwner(): boolean {
    return this.userInfo?.role === 'OWNER' || this.userRole === 'OWNER';
  }

  isLoginPage() {
    return this.router.url === '/login';
  }
  title = 'Gas Inventory Pro';
  sidebarOpen = true;
  profileDropdownOpen = false;
  showAlertDropdown = false;
  alertCount = 0;
  alerts: any[] = [];

  // Font Awesome Icons
  faBars = faBars;
  faTimes = faTimes;
  faChartBar = faChartBar;
  faPlus = faPlus;
  faHistory = faHistory;
  faUsers = faUsers;
  faUser = faUser;
  faBox = faBox;
  faTruck = faTruck;
  faRupeeSign = faRupeeSign;
  faCog = faCog;
  faSearch = faSearch;
  faBell = faBell;
  faChevronDown = faChevronDown;
  faExchange = faExchange;
  faFileAlt = faFileAlt;
  faSignOut = faSignOut;
  faReceipt = faReceipt;
  faExclamationTriangle = faExclamationTriangle;
  faClock = faClock;
  faCheck = faCheck;
  faXmark = faXmark;
  faBook = faBook;

  ngOnInit() {
    console.log('AppComponent ngOnInit - isAuthenticated:', this.isAuthenticated);
    
    // Initialize alerts if user is authenticated (handles page refresh)
    if (this.isAuthenticated) {
      this.alertService.initialize();
    }
    
    this.initializeAlerts();

    // Hide sidebar by default on mobile
    if (window.innerWidth <= 1024) {
      this.sidebarOpen = false;
    } else {
      this.sidebarOpen = true;
    }
    // Hide loading indicator when app initializes
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
      loadingElement.style.display = 'none';
    }
    // Scroll to top on route change
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        // Use setTimeout to ensure DOM is updated before scrolling
        setTimeout(() => {
          // Scroll window to top
          window.scrollTo(0, 0);
          // Scroll main content to top
          const mainContent = document.querySelector('.main-content');
          if (mainContent) {
            mainContent.scrollTop = 0;
          }
          // Also try scrolling the content-wrapper
          const contentWrapper = document.querySelector('.content-wrapper');
          if (contentWrapper) {
            contentWrapper.scrollTop = 0;
          }
        }, 0);
      });
    // Listen for clicks outside the profile dropdown and alert dropdown
    this.globalClickUnlistener = this.renderer.listen('document', 'click', (event: Event) => {
      const target = event.target as HTMLElement;
      
      // Close profile dropdown if clicking outside
      if (this.profileDropdownOpen) {
        const dropdown = document.querySelector('.profile-dropdown');
        const userSection = document.querySelector('.user-section');
        const clickedInsideDropdown = dropdown && dropdown.contains(target);
        const clickedInsideUser = userSection && userSection.contains(target);
        if (!clickedInsideDropdown && !clickedInsideUser) {
          this.profileDropdownOpen = false;
          this.cdr.markForCheck();
        }
      }
      
      // Close alert dropdown if clicking outside
      if (this.showAlertDropdown) {
        const alertDropdown = document.querySelector('.alert-dropdown');
        const bellBtn = document.querySelector('.bell-button');
        
        // Check if click is inside dropdown or bell button
        const isInsideDropdown = alertDropdown && alertDropdown.contains(target);
        const isInsideBell = bellBtn && bellBtn.contains(target);
        
        // Close if clicking outside both
        if (!isInsideDropdown && !isInsideBell) {
          this.showAlertDropdown = false;
          this.cdr.markForCheck();
        }
      }
    });
    // Listen for keyboard shortcuts
    this.renderer.listen('document', 'keydown', (event: KeyboardEvent) => {
      // Only handle shortcuts when Alt is held
      if (event.altKey) {
        switch (event.key.toUpperCase()) {
          case 'D':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/dashboard']));
            break;
          case 'S':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/sales/entry']));
            break;
          case 'H':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/sales/history']));
            break;
          case 'R':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/reports']));
            break;
          case 'I':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/inventory']));
            break;
          case 'P':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/suppliers/transactions']));
            break;
          case 'E':
            event.preventDefault();
            this.ngZone.run(() => this.router.navigate(['/expenses/entry']));
            break;
        }
      }
    });
  }

  ngOnDestroy() {
    if (this.globalClickUnlistener) {
      this.globalClickUnlistener();
      this.globalClickUnlistener = null;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebarOnMobile() {
    if (window.innerWidth <= 1024) {
      this.sidebarOpen = false;
    }
  }

  onSearch(event: KeyboardEvent) {
    // Handle search functionality
    const target = event.target as HTMLInputElement;
  }

  logout() {
    this.alertService.reset();
    this.authService.logout().subscribe({
      next: () => {
        this.ngZone.run(() => {
          localStorage.removeItem('user_info');
          localStorage.removeItem('user_role');
          localStorage.removeItem('user_name');
          this.profileDropdownOpen = false;
          this.cdr.markForCheck();
          this.router.navigate(['/login']);
        });
      },
      error: () => {
        // Even if error, clear local info
        this.ngZone.run(() => {
          localStorage.removeItem('user_info');
          localStorage.removeItem('user_role');
          localStorage.removeItem('user_name');
          this.profileDropdownOpen = false;
          this.cdr.markForCheck();
          this.router.navigate(['/login']);
        });
      }
    });
  }

  toggleProfileDropdown() {
    this.profileDropdownOpen = !this.profileDropdownOpen;
  }

  closeProfileDropdown() {
    this.profileDropdownOpen = false;
  }

  toggleAlertDropdown() {
    this.showAlertDropdown = !this.showAlertDropdown;
  }

  closeAlertDropdown() {
    this.showAlertDropdown = false;
  }

  private initializeAlerts(): void {
    console.log('📢 Initializing alerts subscription in app.component');
    
    this.alertService.getAlertCount$()
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.ngZone.run(() => {
          console.log('📊 Alert count updated:', count);
          this.alertCount = count;
          this.cdr.markForCheck();
        });
      }, error => {
        console.error('❌ Error in alert count subscription:', error);
      });

    this.alertService.getAlerts$()
      .pipe(takeUntil(this.destroy$))
      .subscribe(alerts => {
        this.ngZone.run(() => {
          console.log('📋 Alerts updated:', alerts);
          this.alerts = alerts;
          this.cdr.markForCheck();
        });
      }, error => {
        console.error('❌ Error in alerts subscription:', error);
      });
  }

  dismissAlert(alertId: number, event: Event) {
    event.stopPropagation();
    this.alertService.dismissAlert(alertId).subscribe({
      next: () => {
        this.alerts = this.alerts.filter(a => a.id !== alertId);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error dismissing alert:', err);
      }
    });
  }
}
