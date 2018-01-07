import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AutoFocusDirective} from './auto-focus.directive';

@NgModule({
  imports: [
    CommonModule
  ],
  exports: [
    AutoFocusDirective
  ],
  declarations: [
    AutoFocusDirective
  ]
})
export class SharedModule { }
