import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {AutoFocusDirective} from './auto-focus.directive';
import {ErrorContainerComponent} from './error-container.component';
import {ErrorIndicatorComponent} from './error-indicator.component';
import {ErrorComponent} from './error.component';
import {ImageLoaderComponent} from './image-loader.component';
import {LoadingIndicatorComponent} from './loading-indicator.component';
import {NoContentIndicatorComponent} from './no-content-indicator.component';

@NgModule({
  imports: [
    CommonModule,
    TranslateModule,
  ],
  exports: [
    CommonModule,
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
    TranslateModule,
    LoadingIndicatorComponent,
    ErrorIndicatorComponent,
    NoContentIndicatorComponent,
    ImageLoaderComponent,
  ],
  declarations: [
    AutoFocusDirective,
    ErrorContainerComponent,
    ErrorComponent,
    LoadingIndicatorComponent,
    ErrorIndicatorComponent,
    NoContentIndicatorComponent,
    ImageLoaderComponent,
  ]
})
export class SharedModule {
}
