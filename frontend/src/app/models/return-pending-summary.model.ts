export interface ReturnPendingSummary {
  totalReturnPending: number;
  customersWithReturnPending: number;
  highRiskCount: number;
  pendingReturnThreshold: number | null;
}
