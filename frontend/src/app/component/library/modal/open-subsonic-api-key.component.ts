import {Component, inject, Input} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NotificationService} from '../../../service/notification.service';

@Component({
  imports: [
    TranslateModule
  ],
  selector: 'pony-log',
  templateUrl: './open-subsonic-api-key.component.html',
  styleUrls: ['./open-subsonic-api-key.component.scss']
})
export class OpenSubsonicApiKeyComponent {

  readonly activeModal = inject(NgbActiveModal);

  private readonly notificationService = inject(NotificationService);
  private readonly translateService = inject(TranslateService);

  @Input()
  apiKey!: string;

  isCopyToClipboardAvailable() {
    return navigator?.clipboard?.writeText;
  }

  copyToClipboard() {
    navigator.clipboard.writeText(this.apiKey).then(
      () => {
        this.notificationService.success(
          this.translateService.instant('notification.generateApiKeyTitle'),
          this.translateService.instant('notification.generateApiKeyText')
        );
      },
      () => {
        this.notificationService.error(
          this.translateService.instant('notification.generateApiKeyTitle'),
          this.translateService.instant('notification.generateApiKeyCopyErrorText')
        );
      },
    );
  }
}
