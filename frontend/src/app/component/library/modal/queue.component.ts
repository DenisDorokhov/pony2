import {Component} from "@angular/core";
import {DatePipe, NgForOf, NgIf} from "@angular/common";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [
    DatePipe,
    ErrorIndicatorComponent,
    FormsModule,
    LoadingIndicatorComponent,
    NgForOf,
    NgIf,
    ReactiveFormsModule,
    TranslateModule
  ],
  selector: 'pony-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent {

  constructor(
    public readonly activeModal: NgbActiveModal
  ) {
  }
}
