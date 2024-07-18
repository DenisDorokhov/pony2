import {Component} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";

@Component({
  standalone: true,
  imports: [TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent],
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent {
  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
