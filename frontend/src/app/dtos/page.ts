export interface Page<T> {
  content: T[];
  totalElements: number;
  pageSize: number;
  offset: number;
}
