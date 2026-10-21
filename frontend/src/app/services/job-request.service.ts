import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  JobRequestSearch,
  JobRequestPrice,
  JobRequestImage,
  JobRequest,
  JobRequestWithUser, JobRequestCreateEdit, JobRequestUpdate, /* other DTOs */
} from '../dtos/jobRequest';
import {Page} from "../dtos/page";

const baseUri = environment.backendUrl + "/api/v1/job-requests";

@Injectable({
  providedIn: 'root',
})
export class JobRequestService {

  constructor(private http: HttpClient) {}

  getById(id: number): Observable<JobRequest> {
    return this.http.get<JobRequest>(`${baseUri}/${id}`);
  }

  getByIdWithCustomer(id: number): Observable<JobRequestWithUser> {
    return this.http.get<JobRequestWithUser>(`${baseUri}/${id}/full`);
  }

  listJobRequests():Observable<JobRequest[]> {
    return this.http.get<JobRequest[]>(`${baseUri}/user`);
  }

  create(jobRequest: JobRequestCreateEdit) {
    return this.http.post<JobRequest>(`${baseUri}`, jobRequest);
  }

  update(jobRequest: JobRequestUpdate, id: number): Observable<JobRequest> {
    return this.http.put<JobRequest>(`${baseUri}/${id}/edit`, jobRequest);
  }

  /**
   * Delete a request by its ID
   *
   * @param id The ID of the request to delete
   */
  deleteRequest(id: number): Observable<void>{
    return this.http.delete<void>(`${baseUri}/${id}`);
  }

  listAllRequests(): Observable<JobRequestPrice[]> {
    return this.http.get<JobRequestPrice[]>(`${baseUri}/all`);
  }

  listAllOpenRequests(): Observable<JobRequestPrice[]> {
    return this.http.get<JobRequestPrice[]>(baseUri);
  }

  uploadImage(jobRequestId: number, file: File, displayPosition: number): Observable<JobRequestImage> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('displayPosition', displayPosition.toString());

    return this.http.post<JobRequestImage>(
      `${baseUri}/${jobRequestId}/images`,
      formData
    );
  }

  getImages(jobRequestId: number): Observable<JobRequestImage[]> {
    return this.http.get<JobRequestImage[]>(
      `${baseUri}/${jobRequestId}/images`
    );
  }

  getImageData(jobRequestId: number, imageId: number): Observable<Blob> {
    return this.http.get(
      `${baseUri}/${jobRequestId}/images/${imageId}`,
      { responseType: 'blob' }
    );
  }

  deleteImage(jobRequestId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(
      `${baseUri}/${jobRequestId}/images/${imageId}`
    );
  }

  private createSearchParams(searchParams: JobRequestSearch): HttpParams {
    let params = new HttpParams();
    for (const key in searchParams) {
      if (Object.prototype.hasOwnProperty.call(searchParams, key)) {
        const value = (searchParams as any)[key];
        if (value !== null && value !== undefined && value.toString().trim() !== '') {
          params = params.append(key, value.toString());
        }
      }
    }
    return params;
  }

  searchJobRequestsCustomer(searchParams: JobRequestSearch, offset: number, limit: number): Observable<Page<JobRequestPrice>> {
    const params = this.createSearchParams(searchParams)
      .set('offset', offset.toString())
      .set('limit', limit.toString());
    return this.http.get<Page<JobRequestPrice>>(`${baseUri}/user/search`, { params });
  }

  searchJobRequestsWorker(searchParams: JobRequestSearch, offset: number, limit: number): Observable<Page<JobRequestPrice>> {
    const params = this.createSearchParams(searchParams)
      .set('offset', offset.toString())
      .set('limit', limit.toString());
    return this.http.get<Page<JobRequestPrice>>(`${baseUri}/worker/search`, { params });
  }

  searchJobRequestsAdmin(searchParams: JobRequestSearch, offset: number, limit: number): Observable<Page<JobRequestPrice>> {
    const params = this.createSearchParams(searchParams)
      .set('offset', offset.toString())
      .set('limit', limit.toString());
    return this.http.get<Page<JobRequestPrice>>(`${baseUri}/admin/search`, { params });
  }

  getJobRequestImages(jobRequestId: number): Observable<JobRequestImage[]> {
    return this.http.get<JobRequestImage[]>(`${baseUri}/${jobRequestId}/images`);
  }

  requestDone(jobRequestId: number): Observable<void> {
    return this.http.put<void>(`${baseUri}/${jobRequestId}/done`, null);
  }
}
