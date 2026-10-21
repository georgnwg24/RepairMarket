import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from 'src/environments/environment';
import {User, UserEdit, UserListDto} from "../dtos/user";
import {Page} from "../dtos/page";

const baseUri = environment.backendUrl + '/api/v1/users'

@Injectable({
  providedIn: 'root',
})
export class UserService {

  constructor(private http: HttpClient) {}

  getUserForUpdateByUsername(): Observable<User> {
    return this.http.get<User>(`${baseUri}/edit`);
  }

  getUserDetails(userId: number): Observable<User> {
    return this.http.get<User>(`${baseUri}/${userId}`);
  }
  updateUser( userUpdate: UserEdit): Observable<UserEdit>{
    console.log(userUpdate)
    return this.http.put<UserEdit>(`${baseUri}/edit`, userUpdate);
  }

  /*getAllUsers(): Observable<UserListDto[]> {
    return this.http.get<UserListDto[]>(`${baseUri}`);
  }*/

  getUsers(username?: string): Observable<UserListDto[]> {
    let params = new HttpParams();
    if (username && username.trim().length) {
      params = params.set('username', username.trim());
    }
    return this.http.get<UserListDto[]>(baseUri, { params });
  }

  searchUsers(username: string): Observable<UserListDto[]> {
    return this.getUsers(username);
  }

  getPageOfUsers(offset: number, limit: number, username?: string): Observable<Page<UserListDto>> {
    let params = new HttpParams();
    params = params.set('offset', offset.toString());
    params = params.set('limit', limit.toString());
    if (username && username.trim().length) {
      params = params.set('username', username.trim());
    }
    return this.http.get<Page<UserListDto>>(`${baseUri}`,{ params });
  }


  deleteUser(username: string): Observable<void> {
    return this.http.delete<void>(`${baseUri}/${username}`);
  }


  banUser(id: number): Observable<void> {
    return this.http.patch<void>(`${baseUri}/${id}/ban`, {});
  }

  unbanUser(id: number): Observable<void> {
    return this.http.patch<void>(`${baseUri}/${id}/unban`, {});
  }
}
