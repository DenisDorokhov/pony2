import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ErrorDto} from '../core/error.dto';

@Component({
  selector: 'pony-field-error-container',
  templateUrl: './error-container.component.html',
})
export class ErrorContainerComponent implements OnChanges {

  @Input() error: ErrorDto;
  @Input() field: string;

  errorMessages: string[];

  ngOnChanges(changes: SimpleChanges): void {
    this.errorMessages = this.fetchErrorMessages().sort();
  }

  private fetchErrorMessages(): string[] {
    if (this.field) {
      if (this.error && this.error.fieldViolations) {
        return this.error.fieldViolations
          .filter(fieldViolation => fieldViolation.field === this.field)
          .map(fieldViolation => fieldViolation.message);
      }
    } else if (this.error && this.error.message) {
      return [this.error.message];
    }
    return [];
  }
}
