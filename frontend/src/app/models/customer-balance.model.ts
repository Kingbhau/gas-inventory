export interface CustomerBalance {
  customerId: number;
  customerName: string;
  variantBalances: VariantBalance[];
}

export interface VariantBalance {
  variantId: number;
  variantName: string;
  balance: number;
}
