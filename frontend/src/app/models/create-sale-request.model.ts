export interface SaleItemRequest {
  variantId: number;
  qtyIssued: number;
  qtyEmptyReceived: number;
  discount?: number;
}

export interface SalePaymentSplitRequest {
  modeOfPayment: string;
  amount: number;
  bankAccountId?: number;
  note?: string;
}

export interface CreateSaleRequest {
  customerId: number;
  warehouseId: number;
  saleDate?: string;
  amountReceived?: number;
  modeOfPayment?: string;
  bankAccountId?: number;
  paymentSplits?: SalePaymentSplitRequest[];
  items: SaleItemRequest[];
}
