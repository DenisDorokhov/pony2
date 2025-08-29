import {Component, Input} from '@angular/core';
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

  @Input()
  apiKey!: string;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
  ) {
  }

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
