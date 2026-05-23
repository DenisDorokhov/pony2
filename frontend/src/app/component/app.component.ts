import {Component, inject, ViewContainerRef} from '@angular/core';
import {RouterOutlet} from '@angular/router';

// noinspection JSUnusedGlobalSymbols
@Component({
    selector: 'pony-root',
    imports: [RouterOutlet],
    templateUrl: './app.component.html'
})
export class AppComponent {
  readonly viewContainerRef = inject(ViewContainerRef);
}
