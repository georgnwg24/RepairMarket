import {Injectable} from '@angular/core';
import {AuthRequest} from '../dtos/auth-request';
import {BehaviorSubject, Observable, throwError} from 'rxjs';
import { HttpClient } from '@angular/common/http';
import {map, tap} from 'rxjs/operators';
import {jwtDecode} from 'jwt-decode';
import {Globals} from '../global/globals';
import {UserRegistrationDto} from "../dtos/UserRegistrationDto";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authBaseUri: string = this.globals.backendUri + '/authentication';

  private loggedIn$$ = new BehaviorSubject<boolean>(this.isLoggedIn());
  public loggedIn$ = this.loggedIn$$.asObservable();

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Registers the new user.
   *
   * @param dto Registration form data
   */
  registerUser(dto: UserRegistrationDto): Observable<void> {
    return this.httpClient.post<void>(`${this.authBaseUri}/register`, dto);
  }

  /**
   * Login in the user. If it was successful, a valid JWT token will be stored
   *
   * @param authRequest User data
   */
  loginUser(authRequest: AuthRequest): Observable<string> {
    return this.httpClient.post(this.authBaseUri, authRequest, {responseType: 'text'})
      .pipe(
        // 1. Remove any leading "Bearer " prefix
        map((authResponse: string) =>
          authResponse.replace(/^Bearer\s+/i, '')
        ),
        // 2. Store the cleaned-up token
        tap((rawToken: string) => {
          this.setToken(rawToken);
          this.loggedIn$$.next(true);
        })
      );
  }


  /**
   * Check if a valid JWT token is saved in the localStorage
   */
  isLoggedIn() {
    return !!this.getToken() && (this.getTokenExpirationDate(this.getToken()).valueOf() > new Date().valueOf());
  }

  logoutUser() {
    console.log('Logout');
    localStorage.removeItem('authToken');
    this.loggedIn$$.next(false);
  }

  deleteUserByUsername(): Observable<void> {
    const username = this.getUsername();
    if (!username) {
      return throwError(() => new Error('Not logged in'));
    }
    return this.httpClient.delete<void>(
      `${this.authBaseUri}/${username}`
    );
  }

  getToken() {
    return localStorage.getItem('authToken');
  }

  /**
   * Returns the user id based on the current token
   */
  getId() : number | null {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.id;
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
    return null;
  }

  /**
   * Returns the username based on the current token
   */
  getUsername(): string | null {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.sub;
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
    return null;
  }

  /**
   * Returns the user role based on the current token
   */
  getUserRole() {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        const authInfo: string[] = decoded.rol;
        if (authInfo.includes('ROLE_ADMIN')) {
          return 'ADMIN';
        } else if (authInfo.includes('ROLE_WORKER')) {
          return 'WORKER';
        } else if (authInfo.includes('ROLE_CUSTOMER')) {
          return 'CUSTOMER';
        }
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
    return 'UNDEFINED';
  }

  private setToken(authResponse: string) {
    localStorage.setItem('authToken', authResponse);
  }

  private getTokenExpirationDate(token: string): Date {

    const decoded: any = jwtDecode(token);
    if (decoded.exp === undefined) {
      return null;
    }

    const date = new Date(0);
    date.setUTCSeconds(decoded.exp);
    return date;
  }

}
