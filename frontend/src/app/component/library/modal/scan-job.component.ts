import {Component, Input, OnInit} from "@angular/core";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {ScanJobDto} from "../../../domain/library.dto";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {renderDuration, renderFileSize} from "../../../service/utils";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule],
  selector: 'pony-scan-job',
  templateUrl: './scan-job.component.html',
  styleUrls: ['./scan-job.component.scss']
})
export class ScanJobComponent implements OnInit {

  ScanJobStatus = ScanJobDto.Status;

  @Input()
  scanJob!: ScanJobDto;

  duration: string | undefined;
  songSize: string | undefined;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly translateService: TranslateService,
  ) {
  }

  ngOnInit(): void {
    if (this.scanJob.scanResult) {
      this.duration = renderDuration(this.scanJob.scanResult!.duration, this.translateService);
      this.songSize = renderFileSize(this.scanJob.scanResult!.songSize, this.translateService);
    }
  }
}
