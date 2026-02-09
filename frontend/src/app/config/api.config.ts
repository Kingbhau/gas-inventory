export const API_CONFIG = {
  // Use same-origin API in production (nginx proxies /api to backend).
  baseUrl: '/api',
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
