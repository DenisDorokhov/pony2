import {Component} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap';

@Component({
  selector: 'pony-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss']
})
export class LogComponent {
  constructor(public readonly modalRef: BsModalRef) {
  }
}
