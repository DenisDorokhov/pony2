import {Component, Input, OnChanges} from '@angular/core';
import {ErrorTranslationService} from '../../service/error-translation.service';
import {ErrorDto} from '../../domain/common.dto';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule],
  selector: 'pony-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent implements OnChanges {

  @Input() error: ErrorDto | undefined;
  @Input() ignoredCodes: string[] = [];

  errorMessage: string | undefined;

  constructor(private errorTranslationService: ErrorTranslationService) {
  }

  ngOnChanges() {
    this.errorMessage = this.error ? this.errorTranslationService.translateError(this.error) : null;
  }
}
