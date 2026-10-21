import {BrowserModule} from '@angular/platform-browser';
import {NgModule, isDevMode} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PdfViewerModule } from 'ng2-pdf-viewer';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HeaderComponent} from './components/header/header.component';
import {FooterComponent} from './components/footer/footer.component';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';
import {MessageComponent} from './components/message/message.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {httpInterceptorProviders} from './interceptors';
import {RegistrationComponent} from "./components/registration/registration.component";
import {ToastrModule} from "ngx-toastr";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ConfirmDeleteDialogComponent} from "./components/confirmation-dialogues/confirm-delete-dialog/confirm-delete-dialog.component";
import { ServiceWorkerModule } from '@angular/service-worker';
import {ChatNotificationsComponent} from "./components/chat-notifications/notification.component";
import {ConfirmPopupModule} from "primeng/confirmpopup";
import {ToastModule} from "primeng/toast";
import {ConfirmationService, MessageService} from "primeng/api";
import {providePrimeNG} from "primeng/config";
import presetSchema from "./app-preset-schema";

@NgModule({ declarations: [
        AppComponent,
        HeaderComponent,
        FooterComponent,
        HomeComponent,
        LoginComponent,
        MessageComponent,
    ],
    bootstrap: [AppComponent],
  imports:
    [BrowserModule,
      AppRoutingModule,
      ReactiveFormsModule,
      NgbModule,
      FormsModule,
      ToastrModule.forRoot(),
      BrowserAnimationsModule,
      ConfirmDeleteDialogComponent,
      ServiceWorkerModule.register('ngsw-worker.js', {
        enabled: !isDevMode(),
        registrationStrategy: 'registerWhenStable:3000'
      }),
      PdfViewerModule,
      ChatNotificationsComponent,
      ConfirmPopupModule,
      ToastModule,
      RegistrationComponent,
    ],
    providers: [
      httpInterceptorProviders,
      provideHttpClient(withInterceptorsFromDi()),
      MessageService,
      ConfirmationService,

      providePrimeNG({
        theme: {
          preset: presetSchema,
          options: {
            darkModeSelector: false
          }
        }
      })
    ]
})
export class AppModule {
}
