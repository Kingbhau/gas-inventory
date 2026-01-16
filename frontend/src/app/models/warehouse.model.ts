export interface Warehouse {
  id: number;
  name: string;
  status: string; // ACTIVE, INACTIVE
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface WarehouseDTO extends Warehouse {
}
