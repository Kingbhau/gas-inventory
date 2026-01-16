export interface Customer {
  id?: number;
  name: string;
  mobile: string;
  address: string;
  active: boolean;
  salePrice?: number;
  discountPrice?: number;
  gstNo?: string;
  filledCylinder?: number;
  dueAmount?: number;
  totalPending?: number;
  lastSaleDate?: Date;
  returnPendingUnits?: number;
  filledUnits?: number;
  configuredVariants?: number[];
}
