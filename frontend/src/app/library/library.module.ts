import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
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
