export interface PaymentRequest {
  customerId: number;
  amount: number;
  paymentDate: string;
  paymentMode: string;
  bankAccountId?: number;
}
