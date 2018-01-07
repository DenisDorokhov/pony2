import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {InstalledGuard} from './core/installed-guard.service';
import {NotInstalledGuard} from './core/not-installed-guard.service';

const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', loadChildren: './installation/installation.module#InstallationModule', canLoad: [NotInstalledGuard]},
  {path: 'library', loadChildren: './library/library.module#LibraryModule', canLoad: [InstalledGuard]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
