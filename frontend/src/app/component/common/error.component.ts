import {Component, inject, Input, OnChanges} from '@angular/core';
import {ErrorTranslationService} from '../../service/error-translation.service';
import {ErrorDto} from '../../domain/common.dto';
import {TranslateModule} from '@ngx-translate/core';

@Component({
  imports: [TranslateModule],
  selector: 'pony-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent implements OnChanges {

  private readonly errorTranslationService = inject(ErrorTranslationService);

  @Input() error: ErrorDto | undefined;
  @Input() ignoredCodes: string[] = [];

  errorMessage: string | undefined;

  ngOnChanges() {
    this.errorMessage = this.error ? this.errorTranslationService.translateError(this.error) : null;
  }
}
