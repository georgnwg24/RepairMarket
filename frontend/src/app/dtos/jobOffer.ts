import {JobOfferStatus} from "./JobOfferStatus";

export interface JobOffer {
  id: number;
  comment?: string;
  createdAt: string;
  status: string;
  price: number;
  jobRequestId: number;
  workerId: number;
}

export interface JobOfferCreate {
  price: number;
  comment?: string;
}

export interface JobOfferSummary {
  id: number;
  price: number;
  status: JobOfferStatus;
  jobRequestId: number;
  requestTitle: string;
  lowestPrice: number;
}

export interface JobOfferWithWorker {
  id: number;
  comment?: string;
  createdAt: string;
  status: string;
  price: number;
  jobRequestId: number;
  workerId: number;
  workerUsername: string;
  workerAverageRating?: number;
}
