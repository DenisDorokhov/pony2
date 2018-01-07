import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {LibraryRoutingModule} from './library-routing.module';
import {LibraryComponent} from './library.component';

@NgModule({
  imports: [
    CommonModule,
    LibraryRoutingModule
  ],
  declarations: [
    LibraryComponent
  ]
})
export class LibraryModule {
}
