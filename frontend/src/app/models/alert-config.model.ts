export interface AlertConfig {
  id: number;
  alertType: string;
  enabled: boolean;
  filledCylinderThreshold?: number;
  emptyCylinderThreshold?: number;
  pendingReturnThreshold?: number;
  description: string;
}
