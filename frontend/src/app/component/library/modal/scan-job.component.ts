import {Component, Input} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {ScanJobDto} from "../../../domain/library.dto";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule],
  selector: 'pony-scan-job',
  templateUrl: './scan-job.component.html',
  styleUrls: ['./scan-job.component.scss']
})
export class ScanJobComponent {

  ScanJobStatus = ScanJobDto.Status;

  @Input()
  scanJob!: ScanJobDto;

  duration!: string;
  songSize!: string;
  artworkSize!: string;

  constructor(public readonly activeModal: NgbActiveModal) {
  }
}
