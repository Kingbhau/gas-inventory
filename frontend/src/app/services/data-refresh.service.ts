import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DataRefreshService {
  // Observable sources for different data types
  private dashboardRefreshSubject = new Subject<void>();
  private expenseCreatedSubject = new Subject<any>();
  private saleCreatedSubject = new Subject<any>();
  private paymentReceivedSubject = new Subject<any>();
  private inventoryUpdatedSubject = new Subject<any>();
  private customerUpdatedSubject = new Subject<void>();

  // Public observables
  dashboardRefresh$ = this.dashboardRefreshSubject.asObservable();
  expenseCreated$ = this.expenseCreatedSubject.asObservable();
  saleCreated$ = this.saleCreatedSubject.asObservable();
  paymentReceived$ = this.paymentReceivedSubject.asObservable();
  inventoryUpdated$ = this.inventoryUpdatedSubject.asObservable();
  customerUpdated$ = this.customerUpdatedSubject.asObservable();

  constructor() {}

  // Methods to trigger refresh events
  refreshDashboard() {
    console.log('[DataRefreshService] Dashboard refresh triggered');
    this.dashboardRefreshSubject.next();
  }

  notifyExpenseCreated(expense: any) {
    console.log('[DataRefreshService] Expense created, triggering dashboard refresh', expense);
    this.expenseCreatedSubject.next(expense);
    this.refreshDashboard();
  }

  notifySaleCreated(sale: any) {
    console.log('[DataRefreshService] Sale created, triggering dashboard refresh', sale);
    this.saleCreatedSubject.next(sale);
    this.refreshDashboard();
  }

  notifyPaymentReceived(payment: any) {
    console.log('[DataRefreshService] Payment received, triggering dashboard refresh', payment);
    this.paymentReceivedSubject.next(payment);
    this.refreshDashboard();
  }

  notifyInventoryUpdated(inventory: any) {
    console.log('[DataRefreshService] Inventory updated, triggering dashboard refresh', inventory);
    this.inventoryUpdatedSubject.next(inventory);
    this.refreshDashboard();
  }

  notifyCustomerUpdated() {
    console.log('[DataRefreshService] Customer updated');
    this.customerUpdatedSubject.next();
  }
}
