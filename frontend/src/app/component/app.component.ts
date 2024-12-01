import {Component, ViewContainerRef} from '@angular/core';
import {RouterOutlet} from '@angular/router';

// noinspection JSUnusedGlobalSymbols
@Component({
  standalone: true,
  selector: 'pony-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html'
})
export class AppComponent {
  constructor(
    public viewContainerRef: ViewContainerRef,
  ) {}
}
