import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {SearchAlertCreate, SearchAlertDetail} from "../dtos/searchAlert";

const baseUri = `${environment.backendUrl}/api/v1/search-alerts`;

@Injectable({
  providedIn: 'root'
})
export class SearchAlertService {

  constructor(private http: HttpClient) { }

  createAlert(dto: SearchAlertCreate): Observable<any> {
    return this.http.post(baseUri, dto);
  }

  getUserAlerts(): Observable<SearchAlertDetail[]> {
    return this.http.get<SearchAlertDetail[]>(baseUri);
  }

  deleteAlert(id: number): Observable<void> {
    return this.http.delete<void>(`${baseUri}/${id}`);
  }

  updateAlertStatus(id: number, active: boolean): Observable<void> {
    return this.http.patch<void>(`${baseUri}/${id}`, active);
  }

  resetAlertCount(id: number): Observable<void> {
    return this.http.patch<void>(`${baseUri}/${id}/reset-count`, {});
  }
}
