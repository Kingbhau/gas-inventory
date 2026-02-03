export interface SaleItem {
  id?: number;
  variantId: number;
  variantName: string;
  qtyIssued: number;
  qtyEmptyReceived: number;
  basePrice: number;
  discount: number;
  finalPrice: number;
  amountReceived?: number;
  dueAmount?: number;
}
