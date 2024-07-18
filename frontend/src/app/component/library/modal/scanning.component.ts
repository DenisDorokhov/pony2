import {Component} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
