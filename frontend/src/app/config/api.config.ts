import { environment } from '../../environments/environment';

export const API_CONFIG = {
  baseUrl: environment.apiBaseUrl,
  endpoints: {
    suppliers: '/suppliers',
    customers: '/customers',
    sales: '/sales',
    inventory: '/inventory',
    monthlyPrices: '/monthly-prices',
    supplierTransactions: '/supplier-transactions',
    variants: '/variants',
    ledger: '/ledger',
    warehouses: '/warehouses',
    warehouseTransfers: '/warehouse-transfers',
    bankDeposits: '/bank-deposits'
  }
};

export function getApiUrl(endpoint: string): string {
  return API_CONFIG.baseUrl + endpoint;
}
