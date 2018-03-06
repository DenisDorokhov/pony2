import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {AutoFocusDirective} from './auto-focus.directive';
import {ErrorContainerComponent} from './error-container.component';
import {ErrorComponent} from './error.component';

@NgModule({
  imports: [
    CommonModule,
    TranslateModule,
  ],
  exports: [
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
    TranslateModule,
  ],
  declarations: [
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
  ]
})
export class SharedModule { }
