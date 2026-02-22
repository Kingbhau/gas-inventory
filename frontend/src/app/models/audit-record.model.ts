export interface AuditRecord {
  id?: number;
  entityType: string;
  entityId?: number;
  action: string;
  fieldName?: string;
  oldValue?: string;
  newValue?: string;
  note?: string;
  metadata?: string;
  requestId?: string;
  source?: string;
  createdBy?: string;
  createdDate?: string | Date;
}
