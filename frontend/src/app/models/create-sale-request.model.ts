export interface SaleItemRequest {
  variantId: number;
  qtyIssued: number;
  qtyEmptyReceived: number;
  discount?: number;
}

export interface CreateSaleRequest {
  customerId: number;
  warehouseId: number;
  saleDate?: string;
  amountReceived?: number;
  modeOfPayment?: string;
  bankAccountId?: number;
  items: SaleItemRequest[];
}
