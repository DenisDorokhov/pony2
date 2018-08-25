import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ErrorDto} from '../core/common/common.dto';
import {ErrorTranslationService} from '../core/common/error-translation.service';

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
