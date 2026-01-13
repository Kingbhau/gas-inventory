import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const userInfo = this.auth.getUserInfo();
    
    // First check if user is authenticated
    if (!userInfo) {
      this.router.navigate(['/login']);
      return false;
    }
    
    // Then check role
    const allowedRoles = route.data['roles'] as string[];
    const role = userInfo?.role;
    
    if (allowedRoles && allowedRoles.includes(role)) {
      return true;
    }
    
    // If role doesn't match, redirect based on user's role
    if (role === 'STAFF') {
      this.router.navigate(['/sales/entry']);
    } else {
      // For MANAGER or unknown roles, try dashboard first, fallback to sales/entry
      this.router.navigate(['/dashboard']);
    }
    return false;
  }
}
