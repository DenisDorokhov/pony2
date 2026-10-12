import {Component, inject, Input, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {DiscoveryJobDto, DiscoveryType, LogMessageDto} from '../../../domain/library.dto';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    imports: [TranslateModule, CommonModule],
    selector: 'pony-discovery-job',
    templateUrl: './discovery-job.component.html',
    styleUrls: ['./discovery-job.component.scss']
})
export class DiscoveryJobComponent implements OnInit {

  readonly activeModal = inject(NgbActiveModal);

  DiscoveryJobStatus = DiscoveryJobDto.Status;
  DiscoveryType = DiscoveryType;
  LogMessageLevel = LogMessageDto.Level;

  @Input()
  discoveryJob!: DiscoveryJobDto;

  exception: string | undefined;

  ngOnInit(): void {
    this.exception = LogMessageDto.extractException(this.discoveryJob.logMessage);
  }
}
