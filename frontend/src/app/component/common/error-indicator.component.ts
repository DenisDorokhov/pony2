import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-error-indicator',
  templateUrl: './error-indicator.component.html',
  styleUrls: ['./error-indicator.component.scss']
})
export class ErrorIndicatorComponent {
}
