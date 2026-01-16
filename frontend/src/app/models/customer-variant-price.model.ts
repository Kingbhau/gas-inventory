export interface CustomerVariantPrice {
  id?: number;
  customerId: number;
  variantId: number;
  variantName: string;
  salePrice: number;
  discountPrice: number;
}
