import {Component} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap';

@Component({
  selector: 'pony-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent {
  constructor(public readonly modalRef: BsModalRef) {
  }
}
