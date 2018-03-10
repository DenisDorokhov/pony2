import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {InstalledGuard} from './core/installation/installed-guard.service';
import {NotInstalledGuard} from './core/installation/not-installed-guard.service';
import {AuthenticatedGuard} from './core/user/authenticated-guard.service';
import {NotAuthenticatedGuard} from './core/user/not-authenticated-guard.service';

const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', loadChildren: './installation/installation.module#InstallationModule', canLoad: [NotInstalledGuard]},
  {path: 'library', loadChildren: './library/library.module#LibraryModule', canLoad: [InstalledGuard, AuthenticatedGuard]},
  {path: 'login', loadChildren: './login/login.module#LoginModule', canLoad: [InstalledGuard, NotAuthenticatedGuard]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
