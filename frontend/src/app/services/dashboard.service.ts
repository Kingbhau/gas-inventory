import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, shareReplay, timeout } from 'rxjs/operators';
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
  private dashboardCache$: Observable<DashboardSummary> | null = null;
  private lastFetchTime = 0;
  private cacheExpireTime = 1 * 60 * 1000; // 1 minute (reduced from 5 minutes)
  private cachedYear: number | null = null;
  private cachedMonth: number | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Get comprehensive dashboard data with optional month selection and caching
   * @param year optional year (defaults to current year)
   * @param month optional month 1-12 (defaults to current month)
   * @param forceRefresh bypass cache
   */
  getDashboardSummary(year?: number | null, month?: number | null, forceRefresh: boolean = false): Observable<DashboardSummary> {
    const now = Date.now();
    const isCacheExpired = (now - this.lastFetchTime) > this.cacheExpireTime;
    
    // Check if month/year changed
    const monthYearChanged = this.cachedYear !== year || this.cachedMonth !== month;

    // Refresh if: force refresh, no cache, cache expired, or month/year changed
    const shouldRefresh = forceRefresh || !this.dashboardCache$ || isCacheExpired || monthYearChanged;

    if (shouldRefresh) {
      // Clear cache if force refresh or month/year changed
      if (forceRefresh || monthYearChanged) {
        console.log('[DashboardService] Clearing cache - forceRefresh:', forceRefresh, 'monthYearChanged:', monthYearChanged);
        this.dashboardCache$ = null;
      }

      let url = `${this.apiUrl}/comprehensive`;
      if (year || month) {
        const params = new URLSearchParams();
        if (year) params.append('year', year.toString());
        if (month) params.append('month', month.toString());
        url += '?' + params.toString();
      }

      console.log('[DashboardService] Fetching dashboard data from:', url, 'forceRefresh:', forceRefresh);

      this.dashboardCache$ = this.http
        .get<DashboardSummary>(url)
        .pipe(
          timeout(30000), // 30 second timeout
          shareReplay(1), // Cache the result for subsequent subscribers
          catchError(this.handleError)
        );

      this.lastFetchTime = now;
      this.cachedYear = year || null;
      this.cachedMonth = month || null;
    } else {
      console.log('[DashboardService] Using cached dashboard data');
    }

    return this.dashboardCache$ as Observable<DashboardSummary>;
  }

  /**
   * Force refresh dashboard data (bypass cache)
   */
  refreshDashboard(): Observable<DashboardSummary> {
    console.log('[DashboardService] refreshDashboard() called');
    this.clearCache();
    return this.getDashboardSummary(null, null, true);
  }

  /**
   * Clear cache
   */
  clearCache(): void {
    console.log('[DashboardService] clearCache() called');
    this.dashboardCache$ = null;
    this.lastFetchTime = 0;
    this.cachedYear = null;
    this.cachedMonth = null;
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
