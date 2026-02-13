export interface PageResponse<T> {
  items: T[];
  totalPages: number;
  totalElements: number;
  page: number;
  size: number;
}
