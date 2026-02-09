export interface Warehouse {
  id: number;
  name: string;
  code?: string; // Auto-generated code (e.g., WH001, WH002)
  status: string; // ACTIVE, INACTIVE
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  // Optional relationship collections (loaded when requested)
  inventoryStocks?: any[];
  sales?: any[];
  saleItems?: any[];
  supplierTransactions?: any[];
  fromWarehouseTransfers?: any[];
  toWarehouseTransfers?: any[];
  customerCylinderLedgers?: any[];
}

export interface WarehouseDTO extends Warehouse {
}
