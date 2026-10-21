import { Component } from '@angular/core';
import {AuthService} from "./services/auth.service";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent {
  loggedIn$ = this.authService.loggedIn$;
  title = 'BRP';

  constructor(protected authService: AuthService) {}
}
