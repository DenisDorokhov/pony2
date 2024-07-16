import { Routes } from '@angular/router';
import {NotInstalledGuard} from "./guard/not-installed-guard.service";
import {InstalledGuard} from "./guard/installed-guard.service";
import {AuthenticatedGuard} from "./guard/authenticated-guard.service";
import {NotAuthenticatedGuard} from "./guard/not-authenticated-guard.service";
import {InstallationComponent} from "./component/installation.component";
import {LibraryComponent} from "./component/library.component";
import {LoginComponent} from "./component/login.component";

export const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', component: InstallationComponent, canMatch: [NotInstalledGuard]},
  {path: 'library', component: LibraryComponent, canMatch: [InstalledGuard, AuthenticatedGuard]},
  {path: 'login', component: LoginComponent, canMatch: [InstalledGuard, NotAuthenticatedGuard]},
];
