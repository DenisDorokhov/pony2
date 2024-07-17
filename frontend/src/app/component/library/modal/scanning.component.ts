import {Component} from '@angular/core';
import {NgbActiveModal, NgbProgressbarModule} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  standalone: true,
  imports: [TranslateModule, NgbProgressbarModule],
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
