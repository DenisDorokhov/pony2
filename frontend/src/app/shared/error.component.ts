import {Component, Input} from '@angular/core';
import {ErrorDto} from '../core/error.dto';

@Component({
  selector: 'pony-error',
  templateUrl: './error.component.html',
})
export class ErrorComponent {
  @Input() error: ErrorDto;
  @Input() ignoredCodes: string[] = [];
}
