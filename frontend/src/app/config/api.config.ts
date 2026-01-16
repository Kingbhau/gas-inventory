export const API_CONFIG = {
  baseUrl: 'http://localhost:8080/api',
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
    warehouseTransfers: '/warehouse-transfers'
  }
};

export function getApiUrl(endpoint: string): string {
  return API_CONFIG.baseUrl + endpoint;
}
