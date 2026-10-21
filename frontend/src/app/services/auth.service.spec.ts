import {TestBed} from '@angular/core/testing';

import {AuthService} from './auth.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AuthService', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [RouterTestingModule, ReactiveFormsModule],
    providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
}));

  it('should be created', () => {
    const service: AuthService = TestBed.inject(AuthService);
    expect(service).toBeTruthy();
  });
  afterEach(() => localStorage.removeItem('authToken'));

  it('recovers from a malformed token before service initialization', () => {
    localStorage.setItem('authToken', 'broken-token');
    const service = TestBed.inject(AuthService);
    expect(service.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it('rejects a token without an expiration', () => {
    localStorage.setItem('authToken', 'e30.e30.signature');
    expect(TestBed.inject(AuthService).isLoggedIn()).toBeFalse();
  });
});
