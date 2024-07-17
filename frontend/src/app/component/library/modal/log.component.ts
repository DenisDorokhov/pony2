import {Component} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss']
})
export class LogComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
