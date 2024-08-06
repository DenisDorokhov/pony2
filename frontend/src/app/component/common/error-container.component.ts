import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ErrorDto} from "../../domain/common.dto";
import {ErrorTranslationService} from "../../service/error-translation.service";
import {CommonModule} from "@angular/common";

@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'pony-error-container',
  templateUrl: './error-container.component.html',
  styleUrls: []
})
export class ErrorContainerComponent implements OnChanges {

  @Input() error: ErrorDto | undefined;
  @Input() field: string | undefined;

  errorMessages: string[] = [];
  hasErrors = false;

  constructor(private errorTranslationService: ErrorTranslationService) {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.errorMessages = this.fetchErrorMessages().sort();
    this.hasErrors = this.errorMessages.length > 0;
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
