import {Component, Input} from '@angular/core';

@Component({
  standalone: true,
  selector: 'pony-no-content-indicator',
  templateUrl: './no-content-indicator.component.html',
  styleUrls: ['./no-content-indicator.component.scss']
})
export class NoContentIndicatorComponent {
  @Input() message: string | undefined;
}
