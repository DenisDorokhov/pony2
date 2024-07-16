import {Component} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-loading-indicator',
  templateUrl: './loading-indicator.component.html',
  styleUrls: ['./loading-indicator.component.scss']
})
export class LoadingIndicatorComponent {
}
