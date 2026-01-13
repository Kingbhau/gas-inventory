import { SaleItem } from './sale-item.model';

export interface Sale {
  id?: number;
  customerId: number;
  customerName: string;
  saleDate: string;
  totalAmount: number;
  saleItems: SaleItem[];
}
