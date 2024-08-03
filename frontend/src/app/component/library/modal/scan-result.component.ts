import {Component, Input} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {ScanJobDto} from "../../../domain/library.dto";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule],
  selector: 'pony-scan-result',
  templateUrl: './scan-result.component.html',
  styleUrls: ['./scan-result.component.scss']
})
export class ScanResultComponent {

  @Input()
  scanJob!: ScanJobDto;

  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
