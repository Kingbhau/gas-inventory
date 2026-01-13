// Customer Model
export interface Customer {
  id: string;
  name: string;
  mobile: string;
  address: string;
  createdDate: Date;
  lastSaleDate?: Date;
  totalPending?: number;
}

// Sale Model
export interface Sale {
  id: string;
  customerId: string;
  customerName: string;
  date: Date;
  variantId: string;
  variantName: string;
  filledIssuedQty: number;
  emptyReceivedQty: number;
  basePrice: number;
  discount: number;
  totalAmount: number;
}

// Sale Item (for detailed sales)
export interface SaleItem {
  id: string;
  saleId: string;
  variantId: string;
  quantity: number;
  price: number;
}

// Inventory Stock
export interface InventoryStock {
  id: string;
  variantId: string;
  variantName: string;
  filledQuantity: number;
  emptyQuantity: number;
  lastUpdated: Date;
}

// Cylinder Variant
export interface CylinderVariant {
  id: string;
  name: string;
  weight: number; // in kg (19, 5)
}

// Supplier Transaction
export interface SupplierTransaction {
  id: string;
  date: Date;
  supplierId: string;
  supplierName: string;
  filledReceivedQty: number;
  emptyReturnedQty: number;
  referenceNumber: string;
}

// Monthly Price
export interface MonthlyPrice {
  id: string;
  month: Date;
  variantId: string;
  variantName: string;
  basePrice: number;
}

// Customer Cylinder Ledger (for tracking pending cylinders)
export interface CustomerCylinderLedger {
  id: string;
  customerId: string;
  customerName: string;
  variantId: string;
  variantName: string;
  date: Date;
  filledIssuedQty: number;
  emptyReceivedQty: number;
  runningBalance: number;
}

// Supplier Model
export interface Supplier {
  id: string;
  name: string;
  contactPerson: string;
  mobile: string;
  address: string;
  createdDate: Date;
}

// KPI Data for Dashboard
export interface DashboardKPI {
  filledCylindersInStock: number;
  emptyCylindersInStock: number;
  pendingEmptyCylinders: number;
  todaysSalesAmount: number;
}

// Sales Summary Table Row
export interface SalesSummary {
  variantName: string;
  filledQty: number;
  emptyQty: number;
}

// Pagination
export interface PaginationRequest {
  page: number;
  limit: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginationResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
