import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {JobOfferCreate, JobOffer, JobOfferSummary, JobOfferWithWorker} from '../dtos/jobOffer';
import {Page} from "../dtos/page";

const baseUri = `${environment.backendUrl}/api/v1/job-offers`;

@Injectable({
  providedIn: 'root',
})
export class JobOfferService {
  constructor(private http: HttpClient) {}

  /**
   * Creates a new job offer for the specified job request.
   *
   * @param requestId ID of the job request to offer on
   * @param jobOfferCreate DTO containing price and optional comment
   */
  create(requestId: number, jobOfferCreate: JobOfferCreate): Observable<JobOffer> {
    console.log(jobOfferCreate);
    const formData = new FormData();

    const jobOfferBlob = new Blob([JSON.stringify(jobOfferCreate)], {
      type: 'application/json'
    });
    formData.append('jobOffer', jobOfferBlob);

    return this.http.post<JobOffer>(`${baseUri}/${requestId}/offers`, formData);
  }

  /**
   * Lists all offers sent from the worker with the given ID.
   *
   * @return list of all sent offers
   */
  getOffersFromWorker(offset: number , limit: number): Observable<Page<JobOfferSummary>> {
    let params = new HttpParams()
      .set('offset', offset.toString())
      .set('limit', limit.toString());
    return this.http.get<Page<JobOfferSummary>>(`${baseUri}/worker`, {params});
  }

  getOffers(): Observable<JobOfferSummary[]> {
    return this.http.get<JobOfferSummary[]>(`${baseUri}/all`);
  }

  withdrawOffer(offerId: number): Observable<void> {
    return this.http.post<void>(`${baseUri}/${offerId}/withdraw`, {});
  }

  deleteOffer(offerId: number): Observable<void> {
    return this.http.delete<void>(`${baseUri}/${offerId}`);
  }

  acceptOffer(offer: JobOfferWithWorker): Observable<void> {
    const payload = {
      offerId: offer.id,
      seenCreatedAt: offer.createdAt
    };
    return this.http.post<void>(`${baseUri}/accept`, payload);
  }

  getOffersForCustomer(): Observable<JobOfferWithWorker[]> {
    return this.http.get<JobOfferWithWorker[]>(`${baseUri}/customer`);
  }

  getSingleOffer(offerId: number): Observable<JobOfferCreate> {
    return this.http.get<JobOfferCreate>(`${baseUri}/${offerId}`);
  }

  updateOffer(offerId: number, updatedOffer: JobOfferCreate): Observable<void> {
    const formData = new FormData();

    const jobOfferBlob = new Blob([JSON.stringify(updatedOffer)], {
      type: 'application/json'
    });
    formData.append('jobOffer', jobOfferBlob);

    return this.http.put<void>(`${baseUri}/${offerId}`, formData);
  }
}
