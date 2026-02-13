export interface User {
  id?: number;
  name: string;
  username?: string;
  mobileNo: string;
  role: string;
  active: boolean;
  businessId?: number;
  createdBy?: string;
  createdDate?: string;
  updatedBy?: string;
  updatedDate?: string;
}
