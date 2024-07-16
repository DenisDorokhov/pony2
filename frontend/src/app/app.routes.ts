import { Routes } from '@angular/router';
import {NotInstalledGuard} from "./service/not-installed-guard.service";
import {InstalledGuard} from "./service/installed-guard.service";
import {AuthenticatedGuard} from "./service/authenticated-guard.service";
import {NotAuthenticatedGuard} from "./service/not-authenticated-guard.service";
import {InstallationComponent} from "./component/installation.component";
import {LibraryComponent} from "./component/library/library.component";
import {LoginComponent} from "./component/login.component";

export const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', component: InstallationComponent, canMatch: [NotInstalledGuard]},
  {path: 'library', component: LibraryComponent, canMatch: [InstalledGuard, AuthenticatedGuard]},
  {path: 'login', component: LoginComponent, canMatch: [InstalledGuard, NotAuthenticatedGuard]},
];
