import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { getApiUrl } from '../config/api.config';

export interface DashboardSummary {
  // Today's Data
  todayTotalSales: number;
  todayTotalExpenses: number;
  todayNetProfit: number;
  todayProfitMargin: number;
  todaySalesCount: number;
  todayCashCollected: number;
  todayCollectionRate: number;
  todayCylindersFilled: number;
  todayCylindersEmpty: number;
  todayCylindersTotal: number;
  todayInventoryHealth: number;
  todayReturnsPending: number;
  todayAmountDue: number;
  todayProblematicCustomers: number;

  // Monthly Data
  monthlyTotalSales: number;
  monthlyTotalExpenses: number;
  monthlyNetProfit: number;
  monthlyProfitMargin: number;
  monthlySalesCount: number;
  monthlyCollectionRate: number;
  monthlyProjectedSales: number;
  monthlyProjectedProfit: number;
  daysInMonth: number;
  daysCompleted: number;
  monthlyCylindersSold: number;
  monthlyCylindersReturned: number;
  monthlyInventoryTurnover: number;

  // Previous Months
  previousMonthsMetrics: MonthlyMetric[];
  monthOverMonthGrowth: number;
  yearOverYearGrowth: number;

  // Charts Data
  expenseCategoryBreakdown: { [key: string]: number };
  monthlySalesTrend: DailySalesData[];
  variantSalesBreakdown: VariantSales[];
  inventoryByWarehouse: InventoryHealth[];

  // Insights
  topCustomerToday: string;
  topCategoryExpenseMonthly: string;
  topDebtors: CustomerDue[];
  pendingReturnsDetail: any[];
  salesTrend: string;
  profitTrend: string;
  inventoryTrend: string;
  collectionTrend: string;
  averageDailySales: number;
  averageDailyExpense: number;
  businessInsights: BusinessInsights;
  monthlyData: {
    month: number;
    year: number;
    totalSales: number;
    totalExpenses: number;
  };
  customersDue: CustomerDueDetail[];
  totalActiveCustomers: number;
  customersWithNoDues: number;
  customersWithSlowPayment: number;
  customersWithOverduePayment: number;

  // Alerts
  alerts: DashboardAlert[];
}

export interface MonthlyMetric {
  month: string;
  year: number;
  totalSales: number;
  totalExpenses: number;
  netProfit: number;
  profitMargin: number;
  growthPercentage: number;
}

export interface DailySalesData {
  date: string;
  day: number;
  sales: number;
  expenses: number;
  profit: number;
}

export interface VariantSales {
  variantName: string;
  quantity: number;
  amount: number;
  percentage: number;
}

export interface InventoryHealth {
  warehouseName: string;
  filledCount: number;
  emptyCount: number;
  totalCount: number;
  healthPercentage: number;
}

export interface CustomerDue {
  customerId: number;
  customerName: string;
  dueAmount: number;
  lastTransactionDate: string;
}

export interface CustomerDueDetail {
  customerId: number;
  customerName: string;
  dueAmount: number;
  daysOverdue: number;
  lastPaymentDate: string;
  paymentBehavior: 'REGULAR' | 'IRREGULAR' | 'SLOW' | 'OVERDUED';
}

export interface BusinessInsights {
  monthlyGrowthPercentage: number;
  averageCollectionRate: number;
  averageOrderValue: number;
  inventoryTurnoverRate: number;
  topSellingVariant: string;
  avgProfitMargin: number;
}

export interface DashboardAlert {
  severity: string; // 'critical', 'warning', 'info'
  title: string;
  message: string;
  actionLabel: string;
  actionLink: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = getApiUrl('/dashboard');

  constructor(private http: HttpClient) {}

  /**
   * Get comprehensive dashboard data with optional month selection
   * @param year optional year (defaults to current year)
   * @param month optional month 1-12 (defaults to current month)
   * @param forceRefresh no-op (kept for compatibility)
   */
  getDashboardSummary(year?: number | null, month?: number | null, forceRefresh: boolean = false): Observable<DashboardSummary> {
    let url = `${this.apiUrl}/comprehensive`;
    if (year || month) {
      const params = new URLSearchParams();
      if (year) params.append('year', year.toString());
      if (month) params.append('month', month.toString());
      url += '?' + params.toString();
    }

    return this.http
      .get<DashboardSummary>(url)
      .pipe(
        timeout(30000),
        catchError(this.handleError)
      );
  }

  /**
   * Force refresh dashboard data (bypass cache)
   */
  refreshDashboard(): Observable<DashboardSummary> {
    return this.getDashboardSummary(null, null, true);
  }

  /**
   * Clear cache
   */
  clearCache(): void {
    // No-op: dashboard caching is disabled
  }

  /**
   * Error handling
   */
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred while loading dashboard data';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }

    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}
