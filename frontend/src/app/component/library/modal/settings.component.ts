import {Component} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
