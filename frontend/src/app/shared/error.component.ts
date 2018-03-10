import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ErrorTranslationService} from '../core/common/error-translation.service';
import {ErrorDto} from '../core/common/error.dto';

@Component({
  selector: 'pony-error',
  templateUrl: './error.component.html',
})
export class ErrorComponent implements OnChanges {

  @Input() error: ErrorDto;
  @Input() ignoredCodes: string[] = [];

  errorMessage: string;

  constructor(private errorTranslationService: ErrorTranslationService) {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.errorMessage = this.error ? this.errorTranslationService.translateError(this.error) : null;
  }
}
