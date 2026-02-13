export interface ApiError {
  code: string;
  details: string;
}

export interface ApiResponse<T> {
  message: string;
  data?: T;
  error?: ApiError;
  timestamp: string;
  requestId: string;
}
