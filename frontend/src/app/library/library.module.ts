import {NgModule} from '@angular/core';
import {SharedModule} from '../shared/shared.module';
import {LibraryRoutingModule} from './library-routing.module';
import {LibraryComponent} from './library.component';

@NgModule({
  imports: [
    LibraryRoutingModule,
    SharedModule,
  ],
  declarations: [
    LibraryComponent,
  ]
})
export class LibraryModule {
}
