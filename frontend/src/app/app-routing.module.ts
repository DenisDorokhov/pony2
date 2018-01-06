import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {InstalledGuardService} from './core/installation/installed-guard.service';
import {NotInstalledGuardService} from './core/installation/not-installed-guard.service';

const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', loadChildren: './installation/installation.module#InstallationModule', canLoad: [NotInstalledGuardService]},
  {path: 'library', loadChildren: './library/library.module#LibraryModule', canLoad: [InstalledGuardService]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
