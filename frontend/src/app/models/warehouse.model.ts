export interface Warehouse {
  id: number;
  name: string;
  code?: string; // Auto-generated code (e.g., WH001, WH002)
  status: string; // ACTIVE, INACTIVE
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface WarehouseDTO extends Warehouse {
}
