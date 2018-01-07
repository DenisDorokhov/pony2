import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AutoFocusDirective} from './auto-focus.directive';
import {ErrorContainerComponent} from './error-container.component';
import {ErrorComponent} from './error.component';

@NgModule({
  imports: [
    CommonModule,
  ],
  exports: [
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
  ],
  declarations: [
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
  ]
})
export class SharedModule { }
