import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';
import { RoleGuard } from './services/role.guard';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { SaleEntryComponent } from './pages/sales/sale-entry.component';
import { SalesHistoryComponent } from './pages/sales/sales-history.component';
import { CustomerManagementComponent } from './pages/customers/customer-management.component';
import { InventoryManagementComponent } from './pages/inventory/inventory-management.component';
import { SupplierManagementComponent } from './pages/suppliers/supplier-management.component';
import { SupplierTransactionComponent } from './pages/suppliers/supplier-transaction.component';
import { ReportsComponent } from './pages/reports/reports.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { EmptyReturnComponent } from './pages/empty-return/empty-return.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [RoleGuard], data: { roles: ['MANAGER'] } },
  
  // Sales Routes
  {
    path: 'sales/entry',
    loadComponent: () => import('./pages/sales/sale-entry.component').then(m => m.SaleEntryComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER', 'STAFF'] }
  },
  {
    path: 'sales/history',
    loadComponent: () => import('./pages/sales/sales-history.component').then(m => m.SalesHistoryComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  { path: 'sales', redirectTo: 'sales/entry', pathMatch: 'full' },

  // Expenses Routes
  {
    path: 'expenses/entry',
    loadComponent: () => import('./pages/expenses/expense-entry.component').then(m => m.ExpenseEntryComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  { path: 'expenses', redirectTo: 'expenses/entry', pathMatch: 'full' },
  
  // Customers Routes
  {
    path: 'customers',
    loadComponent: () => import('./pages/customers/customer-management.component').then(m => m.CustomerManagementComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },

  // User Management Route
  {
    path: 'users',
    loadComponent: () => import('./pages/user-management/user-management.component').then(m => m.UserManagementComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  
  // Inventory Routes
  {
    path: 'inventory',
    loadComponent: () => import('./pages/inventory/inventory-management.component').then(m => m.InventoryManagementComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  
  // Suppliers Routes
  {
    path: 'suppliers',
    loadComponent: () => import('./pages/suppliers/supplier-management.component').then(m => m.SupplierManagementComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  {
    path: 'suppliers/transactions',
    loadComponent: () => import('./pages/suppliers/supplier-transaction.component').then(m => m.SupplierTransactionComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  
  // Reports Routes
  {
    path: 'reports',
    loadComponent: () => import('./pages/reports/reports.component').then(m => m.ReportsComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  
  // Settings Routes
  {
    path: 'settings',
    loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER'] }
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER', 'STAFF'] }
  },
  {
    path: 'empty-return',
    loadComponent: () => import('./pages/empty-return/empty-return.component').then(m => m.EmptyReturnComponent),
    canActivate: [RoleGuard],
    data: { roles: ['MANAGER', 'STAFF'] }
  },
  // Catch-all
  { path: '**', redirectTo: 'dashboard' }
];
