import {Component} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [TranslateModule],
  selector: 'pony-current-user',
  templateUrl: './current-user.component.html',
  styleUrls: ['./current-user.component.scss']
})
export class CurrentUserComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
