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

  // Public observables
  dashboardRefresh$ = this.dashboardRefreshSubject.asObservable();
  expenseCreated$ = this.expenseCreatedSubject.asObservable();
  saleCreated$ = this.saleCreatedSubject.asObservable();
  paymentReceived$ = this.paymentReceivedSubject.asObservable();
  inventoryUpdated$ = this.inventoryUpdatedSubject.asObservable();

  constructor() {}

  // Methods to trigger refresh events
  refreshDashboard() {
    this.dashboardRefreshSubject.next();
  }

  notifyExpenseCreated(expense: any) {
    this.expenseCreatedSubject.next(expense);
    this.refreshDashboard();
  }

  notifySaleCreated(sale: any) {
    this.saleCreatedSubject.next(sale);
    this.refreshDashboard();
  }

  notifyPaymentReceived(payment: any) {
    this.paymentReceivedSubject.next(payment);
    this.refreshDashboard();
  }

  notifyInventoryUpdated(inventory: any) {
    this.inventoryUpdatedSubject.next(inventory);
    this.refreshDashboard();
  }
}
