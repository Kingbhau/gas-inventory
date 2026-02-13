import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response';

export function unwrapApiResponse<T>() {
  return map((response: ApiResponse<T>) => {
    if (response?.error) {
      throw new Error(response.error.details || response.message);
    }
    return response?.data as T;
  });
}
