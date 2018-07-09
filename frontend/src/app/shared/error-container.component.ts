import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ErrorTranslationService} from '../core/common/error-translation.service';
import {ErrorDto} from '../core/common/error.dto';

@Component({
  selector: 'pony-error-container',
  templateUrl: './error-container.component.html',
})
export class ErrorContainerComponent implements OnChanges {

  @Input() error: ErrorDto;
  @Input() field: string;

  errorMessages: string[];

  constructor(private errorTranslationService: ErrorTranslationService) {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.errorMessages = this.fetchErrorMessages().sort();
  }

  private fetchErrorMessages(): string[] {
    if (this.field) {
      if (this.error && this.error.fieldViolations) {
        return this.error.fieldViolations
          .filter(fieldViolation => fieldViolation.field === this.field)
          .map(fieldViolation => this.errorTranslationService.translateFieldViolation(fieldViolation));
      }
    } else if (this.error && this.error.message) {
      return [this.errorTranslationService.translateError(this.error)];
    }
    return [];
  }
}
