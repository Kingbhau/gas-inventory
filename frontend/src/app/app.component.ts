import { Component, OnInit, OnDestroy, Renderer2, ChangeDetectorRef, NgZone, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBars, faTimes, faChartBar, faPlus, faHistory, faUsers, faUser, faBox, faTruck,faRupeeSign, faCog, faSearch, faBell, faChevronDown, faExchange, faFileAlt, faSignOut, faReceipt } from '@fortawesome/free-solid-svg-icons';
import { EmptyReturnComponent } from './pages/empty-return/empty-return.component';
import { AuthService } from './services/auth.service';
import { LoadingService, LoadingState } from './services/loading.service';
import { LoaderComponent } from './shared/components/loader.component';
import { HttpClient } from '@angular/common/http';
import { filter } from 'rxjs/operators';
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
  loading$ = this.loadingService.loading$;

  constructor(
    private router: Router,
    private authService: AuthService,
    private loadingService: LoadingService,
    private renderer: Renderer2,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
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

  isLoginPage() {
    return this.router.url === '/login';
  }
  title = 'Gas Inventory Pro';
  sidebarOpen = true;
  profileDropdownOpen = false;

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

  ngOnInit() {
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
      .pipe(filter(event => event instanceof NavigationEnd))
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
    // Listen for clicks outside the profile dropdown
    this.globalClickUnlistener = this.renderer.listen('document', 'click', (event: Event) => {
      if (this.profileDropdownOpen) {
        const dropdown = document.querySelector('.profile-dropdown');
        const btn = document.querySelector('.header-icon-btn[title="Profile Actions"]');
        if (dropdown && !dropdown.contains(event.target as Node) && btn && !btn.contains(event.target as Node)) {
          this.closeProfileDropdown();
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
}
