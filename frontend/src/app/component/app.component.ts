import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';

@Component({
  standalone: true,
  selector: 'pony-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html'
})
export class AppComponent {
}
