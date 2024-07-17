import {Component} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
