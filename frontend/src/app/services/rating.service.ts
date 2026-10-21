import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {Observable} from "rxjs";
import {Rating, RatingStats} from "../dtos/rating";
const baseUri = environment.backendUrl + "/api/v1/ratings"

@Injectable({
  providedIn: 'root'
})
export class RatingService {

  constructor(private http: HttpClient) {
  }

  getRating(jobRequestId: number): Observable<Rating> {
    return this.http.get<Rating>(`${baseUri}/${jobRequestId}`);
  }

  createRating(rating: Rating, jobRequestId: number): Observable<void> {
    return this.http.post<void>(`${baseUri}/${jobRequestId}`, rating);
  }

  updateRating(rating: Rating, jobRequestId: number): Observable<void> {
    return this.http.put<void>(`${baseUri}/${jobRequestId}`, rating);
  }

  getLatestRatings(userId: number): Observable<Rating[]> {
    return this.http.get<Rating[]>(`${baseUri}/user/${userId}`);
  }

  getRatingStats(userId: number): Observable<RatingStats> {
    return this.http.get<RatingStats>(`${baseUri}/user/${userId}/stats`);
  }
}
