export interface Alert {
  id: number;
  alertType: string;
  alertKey: string;
  warehouseId?: number;
  customerId?: number;
  message: string;
  severity: 'warning' | 'critical';
  isDismissed: boolean;
  createdAt: string;
  expiresAt: string;
}
