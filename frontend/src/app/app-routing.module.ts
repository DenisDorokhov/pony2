import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {InstallationGuard} from './core/installation/installation-guard.service';

const routes: Routes = [
  {path: '', redirectTo: 'library', pathMatch: 'full'},
  {path: 'install', loadChildren: './installation/installation.module#InstallationModule'},
  {path: 'library', loadChildren: './library/library.module#LibraryModule', canLoad: [InstallationGuard]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
