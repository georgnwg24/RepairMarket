export interface JobRequest {
  id?: number;
  title: string;
  description?: string;
  category: string;
  status: string;
  deadline: string;
  createdAt?: string;
  customerId?: number;
  propertyId?: number;
  displayImageUrl?: string;
  lowestPrice?: number;
}

export interface JobRequestSearch {
  id?: number;
  title?: string;
  description?: string;
  category?: string;
  status?: string;
  deadline?: string;
  propertyId?: string;
  distance?: number
  lowestPriceMin?: number;
  lowestPriceMax?: number;
}

export interface JobRequestUpdate {
  title: string;
  description?: string;
  category: string;
  status?: string;
  deadline: string;
  propertyId?: number;
}

export interface JobRequestCreateEdit {
  propertyId?: number;
  title: string;
  description?: string;
  category: string;
  status?: string;
  deadline: string;
  jobRequestImages?: JobRequestImage[];
}

export interface JobRequestImage {
  id: number;
  imageType: string;
  displayPosition: number;
  downloadUrl: string;
}

export interface JobRequestPrice {
  id: number;
  title: string;
  description?: string;
  category: string;
  status: string;
  deadline: string;
  lowestPrice: number;
  address: string;
}

export interface JobRequestWithUser {
  id?: number;
  title: string;
  description?: string;
  category: string;
  status: string;
  deadline: string;
  createdAt?: string;
  customerId?: number;
  propertyId?: number;
  customerUsername: string;
}
