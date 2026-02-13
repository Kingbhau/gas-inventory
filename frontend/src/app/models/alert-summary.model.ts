import { Alert } from './alert.model';

export interface AlertSummary {
  alerts: Alert[];
  count: number;
}
