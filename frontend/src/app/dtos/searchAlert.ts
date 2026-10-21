export interface SearchAlertCreate {
  keywords?: string;
  maxDistance?: number;
  categories?: string[];
}

export interface SearchAlertDetail {
  id: number;
  keywords?: string;
  maxDistance?: number;
  categories?: string[];
  active: boolean;
  count: number;
}
