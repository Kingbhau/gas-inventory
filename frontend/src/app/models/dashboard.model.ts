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
  pendingReturnsDetail: unknown[];
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
  severity: string;
  title: string;
  message: string;
  actionLabel: string;
  actionLink: string;
}
