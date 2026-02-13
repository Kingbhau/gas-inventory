export interface CustomerLedgerVariantSummary {
  variantName: string;
  filledCount: number;
  returnPending: number;
}

export interface CustomerLedgerSummary {
  variants: CustomerLedgerVariantSummary[];
}
