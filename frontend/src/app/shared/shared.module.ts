import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AutoFocusDirective} from './auto-focus.directive';
import { ErrorDirective } from './error.directive';

@NgModule({
  imports: [
    CommonModule,
  ],
  exports: [
    AutoFocusDirective,
    ErrorDirective,
  ],
  declarations: [
    AutoFocusDirective,
    ErrorDirective,
  ]
})
export class SharedModule { }
