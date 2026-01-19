import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import {
  faGauge, faBoxes, faShoppingCart, faExclamationTriangle, faEllipsis,
  faArrowTrendUp, faArrowTrendDown, faCalendar, faBell, faCheckCircle,
  faWarning, faCreditCard, faRotateRight
} from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { LoadingService } from '../../services/loading.service';
import { DataRefreshService } from '../../services/data-refresh.service';
import { finalize, Subject, debounceTime } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { DashboardService, DashboardSummary } from '../../services/dashboard.service';

interface KPICard {
  icon: any;
  label: string;
  value: number | string;
  subtext: string;
  color: 'blue' | 'green' | 'orange' | 'red' | 'purple';
  trend?: 'up' | 'down' | 'stable';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, SharedModule, FormsModule, NgChartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  todayDate = new Date();

  // Font Awesome Icons
  faGauge = faGauge;
  faBoxes = faBoxes;
  faShoppingCart = faShoppingCart;
  faExclamationTriangle = faExclamationTriangle;
  faEllipsis = faEllipsis;
  faArrowTrendUp = faArrowTrendUp;
  faArrowTrendDown = faArrowTrendDown;
  faCalendar = faCalendar;
  faBell = faBell;
  faCheckCircle = faCheckCircle;
  faWarning = faWarning;
  faCreditCard = faCreditCard;
  faRotateRight = faRotateRight;

  // Month Selection
  selectedYear: number = new Date().getFullYear();
  selectedMonth: number = new Date().getMonth() + 1; // 1-12
  currentYear: number = new Date().getFullYear();
  currentMonth: number = new Date().getMonth() + 1;
  availableYears: number[] = this.generateAvailableYears();
  monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 
                'July', 'August', 'September', 'October', 'November', 'December'];

  // Dashboard Data
  dashboardData: DashboardSummary | null = null;
  isLoading = true;

  // KPI Cards
  todayKpiCards: KPICard[] = [];
  monthlyKpiCards: KPICard[] = [];


  // Charts
  salesVsExpensesChart: ChartConfiguration<'doughnut'> | null = null;
  monthlySalesTrendChart: ChartConfiguration<'line'> | null = null;
  expenseCategoryChart: ChartConfiguration<'pie'> | null = null;
  variantSalesChart: ChartConfiguration<'bar'> | null = null;
  inventoryStatusChart: ChartConfiguration<'bar'> | null = null;
  topDebtorsChart: ChartConfiguration<'bar'> | null = null;

  // Chart Options
  chartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        padding: 12,
        borderColor: '#e2e8f0',
        borderWidth: 1
      }
    }
  };

  constructor(
    private dashboardService: DashboardService,
    private loadingService: LoadingService,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    private dataRefreshService: DataRefreshService
  ) {}

  ngOnInit() {
    // Load fresh data every time dashboard is opened
    this.loadDashboardData();
    
    // Subscribe to dashboard refresh events with debouncing (500ms) to prevent rapid refreshes
    this.dataRefreshService.dashboardRefresh$
      .pipe(
        debounceTime(500),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        console.log('Dashboard refresh triggered from data-refresh service');
        this.loadDashboardData();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Helper Methods for Date Restrictions
  generateAvailableYears(): number[] {
    const currentYear = new Date().getFullYear();
    return [currentYear - 2, currentYear - 1, currentYear];
  }

  isMonthSelectable(month: number, year: number): boolean {
    // Allow all months for past years
    if (year < this.currentYear) {
      return true;
    }
    // For current year, only allow months up to current month
    if (year === this.currentYear) {
      return month <= this.currentMonth;
    }
    // Block future years
    return false;
  }

  isYearSelectable(year: number): boolean {
    return year <= this.currentYear;
  }

  // Month Navigation Methods
  previousMonth() {
    if (this.selectedMonth === 1) {
      this.selectedMonth = 12;
      this.selectedYear--;
    } else {
      this.selectedMonth--;
    }
    this.loadDashboardData();
  }

  nextMonth() {
    // Prevent navigation to future months
    if (this.selectedYear === this.currentYear && this.selectedMonth === this.currentMonth) {
      return; // Already at current month, don't go forward
    }
    
    if (this.selectedMonth === 12) {
      this.selectedMonth = 1;
      this.selectedYear++;
      // If year becomes future year, revert
      if (this.selectedYear > this.currentYear) {
        this.selectedYear--;
        this.selectedMonth = 12;
        return;
      }
    } else {
      this.selectedMonth++;
      // If selected month goes beyond current month in current year, revert
      if (this.selectedYear === this.currentYear && this.selectedMonth > this.currentMonth) {
        this.selectedMonth--;
        return;
      }
    }
    this.loadDashboardData();
  }

  selectMonth(month: number) {
    this.selectedMonth = month;
    this.loadDashboardData();
  }

  onMonthChange(newMonth: number) {
    this.selectedMonth = newMonth;
    this.loadDashboardData();
  }

  onYearChange(newYear: number) {
    this.selectedYear = newYear;
    this.loadDashboardData();
  }

  getCurrentMonthDisplay(): string {
    const today = new Date();
    const currentMonth = today.getMonth() + 1;
    const currentYear = today.getFullYear();
    
    if (this.selectedMonth === currentMonth && this.selectedYear === currentYear) {
      return 'This Month';
    }
    return `${this.monthNames[this.selectedMonth - 1]} ${this.selectedYear}`;
  }

  loadDashboardData() {
    this.isLoading = true;
    this.loadingService.show('Loading dashboard for ' + this.monthNames[this.selectedMonth - 1] + '...');

    this.dashboardService.getDashboardSummary(this.selectedYear, this.selectedMonth)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loadingService.hide();
          this.isLoading = false;
        })
      )
      .subscribe({
        next: (data) => {
          this.dashboardData = data;
          this.processDashboardData(data);
          this.cdr.markForCheck();
        },
        error: (error) => {
          const errorMessage = error?.message || 'Error loading dashboard data';
          this.toastr.error(errorMessage, 'Error');
          console.error('Dashboard error:', error);
        }
      });
  }

  private processDashboardData(data: DashboardSummary) {
    try {
      // Build TODAY KPI Cards
      this.buildTodayKpiCards(data);

      // Build MONTHLY KPI Cards
      this.buildMonthlyKpiCards(data);

      // Build CHARTS
      this.buildCharts(data);
      
      // Trigger change detection to update charts
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error processing dashboard data:', error);
      this.toastr.error('Error processing dashboard data', 'Error');
    }
  }

  private buildTodayKpiCards(data: DashboardSummary) {
    const salesCount = data?.todaySalesCount || 0;
    const totalSales = data?.todayTotalSales || 0;
    const totalExpenses = data?.todayTotalExpenses || 0;
    const netProfit = data?.todayNetProfit || 0;
    const profitMargin = data?.todayProfitMargin || 0;
    const collectionRate = data?.todayCollectionRate || 0;
    const cashCollected = data?.todayCashCollected || 0;
    const inventoryHealth = data?.todayInventoryHealth || 0;
    const cylindersFilled = data?.todayCylindersFilled || 0;
    const cylindersEmpty = data?.todayCylindersEmpty || 0;
    const returnsPending = data?.todayReturnsPending || 0;
    const problematicCustomers = data?.todayProblematicCustomers || 0;
    const amountDue = data?.todayAmountDue || 0;

    this.todayKpiCards = [
      {
        icon: faShoppingCart,
        label: 'Today Sales',
        value: salesCount,
        subtext: `₹${this.formatCurrency(totalSales)} | ${salesCount} transactions`,
        color: 'green',
        trend: 'up'
      },
      {
        icon: faExclamationTriangle,
        label: 'Today Expenses',
        value: totalExpenses.toFixed(0),
        subtext: `₹${this.formatCurrency(totalExpenses)}`,
        color: 'orange'
      },
      {
        icon: faGauge,
        label: 'Today Profit',
        value: netProfit.toFixed(0),
        subtext: `₹${this.formatCurrency(netProfit)} | Margin: ${profitMargin.toFixed(1)}%`,
        color: netProfit > 0 ? 'green' : 'red',
        trend: netProfit > 0 ? 'up' : 'down'
      },
      {
        icon: faCreditCard,
        label: 'Collection Rate',
        value: `${collectionRate.toFixed(1)}%`,
        subtext: `Collected: ₹${this.formatCurrency(cashCollected)}`,
        color: collectionRate > 70 ? 'green' : 'orange'
      },
      {
        icon: faBoxes,
        label: 'Inventory Health',
        value: `${inventoryHealth.toFixed(1)}%`,
        subtext: `${cylindersFilled} filled, ${cylindersEmpty} empty`,
        color: inventoryHealth > 60 ? 'green' : 'orange'
      },
      {
        icon: faWarning,
        label: 'Pending Returns',
        value: returnsPending,
        subtext: `cylinders awaiting pickup`,
        color: returnsPending > 50 ? 'red' : 'orange'
      },
      {
        icon: faBell,
        label: 'Amount Due',
        value: problematicCustomers,
        subtext: `₹${this.formatCurrency(amountDue)} from customers`,
        color: 'red'
      },
      {
        icon: faCalendar,
        label: 'Top Customer',
        value: data.topCustomerToday || 'N/A',
        subtext: 'Today\'s highest sale',
        color: 'blue'
      }
    ];
  }

  private buildMonthlyKpiCards(data: DashboardSummary) {
    this.monthlyKpiCards = [
      {
        icon: faShoppingCart,
        label: 'Monthly Sales',
        value: data.monthlySalesCount,
        subtext: `₹${this.formatCurrency(data.monthlyTotalSales)} (${data.daysCompleted}/${data.daysInMonth} days)`,
        color: 'green',
        trend: data.monthOverMonthGrowth && data.monthOverMonthGrowth > 0 ? 'up' : 'down'
      },
      {
        icon: faExclamationTriangle,
        label: 'Monthly Expenses',
        value: `₹${this.formatCurrency(data.monthlyTotalExpenses)}`,
        subtext: `${((data.monthlyTotalExpenses / data.monthlyTotalSales) * 100).toFixed(1)}% of sales`,
        color: 'orange'
      },
      {
        icon: faGauge,
        label: 'Monthly Profit',
        value: data.monthlyNetProfit.toFixed(0),
        subtext: `₹${this.formatCurrency(data.monthlyNetProfit)} | Projected: ₹${this.formatCurrency(data.monthlyProjectedProfit)}`,
        color: 'green'
      },
      {
        icon: faCalendar,
        label: 'Projected This Month',
        value: data.daysInMonth,
        subtext: `₹${this.formatCurrency(data.monthlyProjectedSales)} sales`,
        color: 'blue'
      }
    ];
  }

  private buildCharts(data: DashboardSummary) {
    try {
      // 1. Sales vs Expenses Doughnut Chart
      if (data.todayTotalSales > 0 || data.todayTotalExpenses > 0) {
        this.salesVsExpensesChart = {
          type: 'doughnut',
          data: {
            labels: ['Sales', 'Expenses'],
            datasets: [{
              data: [data.todayTotalSales, data.todayTotalExpenses],
              backgroundColor: ['#10b981', '#f59e0b'],
              borderColor: '#ffffff',
              borderWidth: 2
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { position: 'bottom' },
              tooltip: {
                callbacks: {
                  label: (context: any) => {
                    const value = context.parsed || 0;
                    return `₹${this.formatCurrency(Number(value))}`;
                  }
                }
              }
            }
          } as any
        };
      }

      // 2. Inventory Status Bar Chart
      if (data.inventoryByWarehouse && data.inventoryByWarehouse.length > 0) {
        const warehouseNames = data.inventoryByWarehouse.map(w => w.warehouseName);
        const filledData = data.inventoryByWarehouse.map(w => w.filledCount);
        const emptyData = data.inventoryByWarehouse.map(w => w.emptyCount);

        this.inventoryStatusChart = {
          type: 'bar',
          data: {
            labels: warehouseNames,
            datasets: [
              {
                label: 'Filled Cylinders',
                data: filledData,
                backgroundColor: '#10b981',
                borderColor: '#059669',
                borderWidth: 1,
                borderRadius: 4
              },
              {
                label: 'Empty Cylinders',
                data: emptyData,
                backgroundColor: '#f87171',
                borderColor: '#dc2626',
                borderWidth: 1,
                borderRadius: 4
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: 'x',
            plugins: {
              legend: { 
                position: 'bottom',
                labels: { usePointStyle: true, padding: 15 }
              },
              tooltip: {
                callbacks: {
                  label: function(context: any) {
                    return context.dataset.label + ': ' + context.parsed.y + ' cylinders';
                  }
                }
              }
            },
            scales: {
              x: { stacked: false, grid: { display: false } },
              y: { 
                stacked: false, 
                grid: { display: false },
                ticks: {
                  callback: function(value: any) {
                    return value + ' units';
                  }
                }
              }
            }
          } as any
        };
      }

      // 3. Expense Category Pie Chart
      if (data.expenseCategoryBreakdown && Object.keys(data.expenseCategoryBreakdown).length > 0) {
        const categories = Object.keys(data.expenseCategoryBreakdown);
        const amounts = Object.values(data.expenseCategoryBreakdown);

        this.expenseCategoryChart = {
          type: 'pie',
          data: {
            labels: categories,
            datasets: [{
              data: amounts,
              backgroundColor: [
                '#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444',
                '#8b5cf6', '#ec4899', '#14b8a6', '#0ea5e9', '#6366f1'
              ],
              borderColor: '#ffffff',
              borderWidth: 2
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { position: 'right' },
              tooltip: {
                callbacks: {
                  label: (context: any) => {
                    const value = context.parsed || 0;
                    return `₹${this.formatCurrency(value)}`;
                  }
                }
              }
            }
          } as any
        };
      } else {
        // Clear chart if no expense data
        this.expenseCategoryChart = null;
      }

      // 4. Top Debtors Bar Chart
      if (data.topDebtors && data.topDebtors.length > 0) {
        const debtorNames = data.topDebtors.map(d => d.customerName);
        const dueAmounts = data.topDebtors.map(d => d.dueAmount);

        this.topDebtorsChart = {
          type: 'bar',
          data: {
            labels: debtorNames,
            datasets: [{
              label: 'Amount Due (₹)',
              data: dueAmounts,
              backgroundColor: '#ef4444',
              borderColor: '#dc2626',
              borderWidth: 1
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: 'x',
            plugins: {
              legend: { position: 'bottom' },
              tooltip: {
                callbacks: {
                  label: (context: any) => {
                    const value = context.parsed.y || 0;
                    return `₹${this.formatCurrency(Number(value))}`;
                  }
                }
              }
            },
            scales: {
              x: { grid: { display: false } },
              y: {
                beginAtZero: true,
                grid: { display: false },
                ticks: {
                  callback: (value: any) => `₹${this.formatCurrency(Number(value))}`
                }
              }
            }
          } as any
        };
      }

      // 5. Variant Sales Chart (if data available, else show placeholder)
      if (data.variantSalesBreakdown && data.variantSalesBreakdown.length > 0) {
        const variants = data.variantSalesBreakdown.map((v: any) => v.variantName);
        const sales = data.variantSalesBreakdown.map((v: any) => v.quantity || v.salesAmount);

        this.variantSalesChart = {
          type: 'bar',
          data: {
            labels: variants,
            datasets: [{
              label: 'Cylinders Inventory (count)',
              data: sales,
              backgroundColor: '#06b6d4',
              borderColor: '#0891b2',
              borderWidth: 1
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { position: 'bottom' }
            },
            scales: {
              x: { grid: { display: false } },
              y: { grid: { display: false } }
            }
          } as any
        };
      } else {
        // Placeholder for no variant data
        this.variantSalesChart = {
          type: 'bar',
          data: {
            labels: ['No Data'],
            datasets: [{
              label: 'No variant data available',
              data: [0],
              backgroundColor: '#f3f4f6',
              borderColor: '#d1d5db',
              borderWidth: 1
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
          } as any
        };
      }

      // Build Daily Sales Trend Chart
      if (this.dashboardData?.monthlySalesTrend && this.dashboardData.monthlySalesTrend.length > 0) {
        const dates = this.dashboardData.monthlySalesTrend.map((item: any) => item.date);
        const salesData = this.dashboardData.monthlySalesTrend.map((item: any) => item.sales);
        const profitData = this.dashboardData.monthlySalesTrend.map((item: any) => item.profit);

        this.monthlySalesTrendChart = {
          type: 'line',
          data: {
            labels: dates,
            datasets: [
              {
                label: 'Daily Sales',
                data: salesData,
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                borderWidth: 2,
                tension: 0.3,
                fill: false,
                pointBackgroundColor: '#10b981',
                pointBorderColor: '#10b981',
                pointRadius: 4,
                pointHoverRadius: 6
              },
              {
                label: 'Daily Profit',
                data: profitData,
                borderColor: '#3b82f6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 2,
                tension: 0.3,
                fill: false,
                pointBackgroundColor: '#3b82f6',
                pointBorderColor: '#3b82f6',
                pointRadius: 4,
                pointHoverRadius: 6
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'bottom',
                labels: { 
                  usePointStyle: true,
                  padding: 15,
                  font: { size: 12, weight: 'bold' as any }
                }
              },
              filler: { propagate: true }
            },
            scales: {
              x: {
                grid: { display: false },
                ticks: { maxTicksLimit: 8 }
              },
              y: {
                grid: { display: false },
                ticks: {
                  callback: (value: any) => '₹' + this.formatCurrency(value as number)
                }
              }
            }
          } as any
        };
      } else {
        // Placeholder for no data
        this.monthlySalesTrendChart = {
          type: 'line',
          data: {
            labels: ['No Data'],
            datasets: [{
              label: 'No trend data available',
              data: [0],
              borderColor: '#d1d5db',
              backgroundColor: '#f3f4f6'
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
          } as any
        };
      }

    } catch (error) {
      console.error('Error building charts:', error);
    }
  }

  // Utility Methods
  private formatCurrency(value: number): string {
    if (isNaN(value)) return '0';
    return Math.round(value).toLocaleString('en-IN');
  }

  private getColorPalette(count: number): string[] {
    const palette = [
      '#3b82f6', '#1e40af', '#1d4ed8',
      '#16a34a', '#15803d', '#166534',
      '#ea580c', '#d97706', '#b45309',
      '#dc2626', '#b91c1c', '#7f1d1d',
      '#8b5cf6', '#7c3aed', '#6d28d9',
      '#06b6d4', '#0891b2', '#0e7490'
    ];

    const result: string[] = [];
    for (let i = 0; i < count; i++) {
      result.push(palette[i % palette.length]);
    }
    return result;
  }

  // Event Handlers
  refreshDashboard() {
    this.loadingService.show('Refreshing dashboard...');
    this.dashboardService.refreshDashboard()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingService.hide())
)
      .subscribe({
        next: (data) => {
          this.dashboardData = data;
          this.processDashboardData(data);
          this.toastr.success('Dashboard refreshed successfully', 'Success');
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.toastr.error('Error refreshing dashboard', 'Error');
          console.error('Refresh error:', error);
        }
      });
  }

  getTrendIcon(trend?: string) {
    switch (trend) {
      case 'up':
        return this.faArrowTrendUp;
      case 'down':
        return this.faArrowTrendDown;
      default:
        return null;
    }
  }

  getTrendClass(trend?: string): string {
    switch (trend) {
      case 'up':
        return 'trend-up';
      case 'down':
        return 'trend-down';
      default:
        return '';
    }
  }

  getAlertSeverityClass(severity: string): string {
    switch (severity) {
      case 'critical':
        return 'alert-critical';
      case 'warning':
        return 'alert-warning';
      case 'info':
        return 'alert-info';
      default:
        return 'alert-info';
    }
  }

  getAlertIcon(severity: string) {
    switch (severity) {
      case 'critical':
      case 'warning':
        return this.faWarning;
      case 'info':
        return this.faCheckCircle;
      default:
        return this.faBell;
    }
  }

  onChartClick(event: any) {
    // Handle chart click events if needed
    console.log('Chart clicked:', event);
  }
}
