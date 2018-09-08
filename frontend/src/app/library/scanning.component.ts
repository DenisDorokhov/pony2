import {Component} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap';

@Component({
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent {
  constructor(public readonly modalRef: BsModalRef) {
  }
}
