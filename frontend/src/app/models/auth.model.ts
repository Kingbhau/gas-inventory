export interface AuthLoginResponse {
  id: number;
  username: string;
  name: string;
  role: string;
  mobileNo?: string;
  businessId?: number;
}

export interface AuthRegisterResponse {
  id: number;
  username: string;
  name: string;
  role: string;
  mobileNo?: string;
  businessId?: number;
  active?: boolean;
}

export interface AuthUserInfo {
  id: number;
  username: string;
  name: string;
  role: string;
  mobileNo?: string;
  businessId?: number | null;
}
