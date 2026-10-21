import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'src/environments/environment';
import {License} from "../dtos/license";
import {Page} from "../dtos/page";

const baseUri = environment.backendUrl + "/api/v1/licenses"

@Injectable({
  providedIn: 'root',
})
export class LicenseService {

  constructor(private http: HttpClient) {
  }

  getById(id: number): Observable<License> {
    return this.http.get<License>(`${baseUri}/${id}`);
  }

  listCertificate(username: string): Observable<License[]> {
    return this.http.get<License[]>(`${baseUri}/user/${username}`);
  }

  /**
   * Fetch a paginated list of pending licenses
   */
  getPendingLicensesPage(offset: number, limit: number, searchTerm?: string): Observable<Page<License>> {
    let params = new HttpParams().set('offset', offset.toString()).set('limit', limit.toString());
    if (searchTerm && searchTerm.trim().length) {
      console.log("searchTerm: " + searchTerm + "");
      params = params.set('username', searchTerm.trim());
    }
    return this.http.get<Page<License>>(`${baseUri}/pending`, {params});
  }


  downloadLicenseFile(id: number): Observable<Blob> {

    const headers = new HttpHeaders({});

    return this.http.get(`${baseUri}/${id}/file`, {
      headers: headers,
      responseType: 'blob',
    });
  }

  create(license: FormData): Observable<License> {
    return this.http.post<License>(`${baseUri}`, license);
  }

  update(license: FormData, id: number): Observable<License> {
    return this.http.put<License>(`${baseUri}/${id}/edit`, license);
  }

  deleteCertificate(licenseId: number): Observable<License> {
    return this.http.delete<License>(
      `${baseUri}/${licenseId}`
    );
  }

  /**
   * Approve or reject a pending license
   * @param id     The ID of the license to update
   * @param status The new status ('APPROVED' or 'REJECTED')
   */
  updateStatus(id: number, status: string): Observable<License> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<License>(`${baseUri}/${id}/status`, null, {params});
  }

  getApprovedLicensesPage(offset: number, limit: number, searchTerm?: string): Observable<Page<License>> {

    let params = new HttpParams().set('offset', offset.toString()).set('limit', limit.toString());
    if (searchTerm && searchTerm.trim().length) {
      console.log("searchTerm: " + searchTerm + "");
      params = params.set('username', searchTerm.trim());
    }
    return this.http.get<Page<License>>(`${baseUri}/approved`, {params});
  }

  /**
   * Fetch a paginated list of rejected licenses
   */
  getRejectedLicensesPage(offset: number, limit: number, searchTerm?: string): Observable<Page<License>> {
    let params = new HttpParams().set('offset', offset.toString()).set('limit', limit.toString());
    if (searchTerm && searchTerm.trim().length) {
      params = params.set('username', searchTerm.trim());
    }
    return this.http.get<Page<License>>(`${baseUri}/rejected`, {params});
  }

}
