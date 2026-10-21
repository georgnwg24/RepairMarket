export interface Rating {
  id?: number;
  stars: number;
  comment: string;
}

export interface RatingStats {
  average: number;
  count: number;
}
