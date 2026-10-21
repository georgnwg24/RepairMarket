import {NgModule} from '@angular/core';
import {mapToCanActivate, RouterModule, Routes} from '@angular/router';
import {LoginComponent} from './components/login/login.component';
import {AuthGuard} from './guards/auth.guard';
import {MessageComponent} from './components/message/message.component';
import {RegistrationComponent} from "./components/registration/registration.component";
import {
  JobRequestCreateEditMode,
  JobRequestCreateEditComponent
} from "./components/job-request-create-edit/job-request-create-edit.component";
import {JobOfferCreateEditComponent} from "./components/job-offer-create-edit/job-offer-create-edit.component";
import {WorkerSentOffersComponent} from "./components/worker-sent-offers/worker-sent-offers.component";
import {UserEditComponent} from "./components/user-edit/user-edit.component";
import {JobRequestDetailComponent} from "./components/job-request-detail/job-request-detail.component";
import {Role} from "./dtos/UserRegistrationDto";
import {
  PropertiesCreateEditComponent,
  PropertyCreateEditMode
} from "./components/properties-create-edit/properties-create-edit.component";
import {AdminMainPageComponent} from "./components/admin-main-page/admin-main-page.component";
import { UserViewComponent } from "./components/user-view/user-view.component";
import {
  LicenseCreateEditMode,
  LicenseCreateEditComponent
} from "./components/license-create-edit/license-create-edit.component";
import {RatingsCreateEditComponent} from "./components/ratings-create-edit/ratings-create-edit.component";
import {UserDetailComponent} from "./components/user-details/user-detail.component";
import {ChatComponent} from "./components/chat/chat.component";
import {AdminLicenseViewComponent} from "./components/admin-license-view/admin-license-view.component";
import {SavedSearchesComponent} from "./components/saved-searches/saved-searches.component";
import {LandingPageComponent} from "./components/landing-page/landing-page.component";
import {JobRequestGalleryComponent} from "./components/job-request-gallery/job-request-gallery.component";
import {ReportDetailComponent} from "./components/report-detail/report-detail.component";
import { OfferCreationGuard } from './guards/offer-creation.guard';
import { OfferEditGuard } from './guards/offer-edit.guard';
import { RequestEditGuard } from './guards/request-edit.guard';


const routes: Routes = [
  {path: '', component: LandingPageComponent},
  {path: 'login', component: LoginComponent},
  {path: 'message', canActivate: mapToCanActivate([AuthGuard]), component: MessageComponent},
  {path: 'register', component: RegistrationComponent},
  { path: 'chats', canActivate: mapToCanActivate([AuthGuard]), component: ChatComponent },
  { path: 'chats/:chatId', canActivate: mapToCanActivate([AuthGuard]), component: ChatComponent },
  { path: 'admin/reports', canActivate: mapToCanActivate([AuthGuard]), data: {requiredRole: 'ADMIN'}, component: AdminMainPageComponent },
  { path: 'admin/reports/:reportId', canActivate: mapToCanActivate([AuthGuard]), data: {requiredRole: 'ADMIN'}, component: ReportDetailComponent },
  { path: 'admin/users', canActivate: mapToCanActivate([AuthGuard]), data: {requiredRole: 'ADMIN'}, component: UserViewComponent },
  {
    path: 'admin/requests',
    canActivate: mapToCanActivate([AuthGuard]),
    data: {requiredRole: 'ADMIN', mode: 'admin'},
    component: JobRequestGalleryComponent,
  },
  {
    path: 'admin/requests/:requestId/details',
    canActivate: mapToCanActivate([AuthGuard]),
    data: {requiredRole: 'ADMIN'},
    component: JobRequestDetailComponent
  },
  { path: 'admin/licenses', canActivate: mapToCanActivate([AuthGuard]), data: {requiredRole: 'ADMIN'}, component: AdminLicenseViewComponent },
  {path: 'profile', children: [
      {
        path: 'worker/:userId',
        canActivate: mapToCanActivate([AuthGuard]),
        component: UserDetailComponent,
        data: {mode: Role.WORKER}
      },
      {
        path: 'customer/:userId',
        canActivate: mapToCanActivate([AuthGuard]),
        component: UserDetailComponent,
        data: {mode: Role.CUSTOMER }
      }
    ]},
  {
    path: `customer`, children: [
      {path: '', redirectTo: 'requests', pathMatch: 'full'},
      {
        path: 'requests',
        component: JobRequestGalleryComponent,
        data: { mode: 'customer', requiredRole: Role.CUSTOMER },
        canActivate: mapToCanActivate([AuthGuard])
      },
      {
        path: 'requests/:requestId/details',
        component: JobRequestDetailComponent,
        data: { requiredRole: Role.CUSTOMER },
        canActivate: mapToCanActivate([AuthGuard])
      },
      {
        path: 'edit',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {mode: Role.CUSTOMER, requiredRole: Role.CUSTOMER},
        component: UserEditComponent
      },
      {
        path: 'address/create',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {mode: PropertyCreateEditMode.create, requiredRole: Role.CUSTOMER},
        component: PropertiesCreateEditComponent
      },
      {
        path: 'address/:id/edit',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {mode: PropertyCreateEditMode.edit, requiredRole: Role.CUSTOMER},
        component: PropertiesCreateEditComponent
      },
      {
        path: 'requests/create',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {mode: JobRequestCreateEditMode.create, requiredRole: Role.CUSTOMER},
        component: JobRequestCreateEditComponent
      },
      {
        path: 'requests/:id/edit',
        canActivate: mapToCanActivate([AuthGuard, RequestEditGuard]),
        data: {mode: JobRequestCreateEditMode.edit, requiredRole: Role.CUSTOMER},
        component: JobRequestCreateEditComponent
      },
      {
        path: 'rating/:requestId',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.CUSTOMER},
        component: RatingsCreateEditComponent
      },
    ]
  },
  {
    path: 'worker', children: [
      {path: '', redirectTo: 'requests', pathMatch: 'full'},
      {
        path: 'requests',
        component: JobRequestGalleryComponent,
        data: { mode: 'worker', requiredRole: Role.WORKER },
        canActivate: mapToCanActivate([AuthGuard])
      },
      {
        path: 'requests/:requestId/details',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER},
        component: JobRequestDetailComponent
      },
      {
        path: 'edit',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {mode: Role.WORKER, requiredRole: Role.WORKER},
        component: UserEditComponent
      },
      {
        path: 'requests/:requestId/offers/create',
        canActivate: mapToCanActivate([AuthGuard, OfferCreationGuard]),
        data: {requiredRole: Role.WORKER},
        component: JobOfferCreateEditComponent
      },
      {
        path: 'offers',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER},
        component: WorkerSentOffersComponent
      },
      {
        path: 'offers/:offerId/edit',
        canActivate: mapToCanActivate([AuthGuard, OfferEditGuard]),
        data: {requiredRole: Role.WORKER},
        component: JobOfferCreateEditComponent
      },
      {
        path: 'license/create',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER, mode: LicenseCreateEditMode.create},
        component: LicenseCreateEditComponent
      },
      {
        path: 'license/:id/edit',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER, mode: LicenseCreateEditMode.edit},
        component: LicenseCreateEditComponent
      },
      {
        path: 'rating/:requestId',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER},
        component: RatingsCreateEditComponent
      },
      {
        path: 'saved-searches',
        canActivate: mapToCanActivate([AuthGuard]),
        data: {requiredRole: Role.WORKER},
        component: SavedSearchesComponent
      },
    ]
  },
  { path: '**', component: LandingPageComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: false})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
