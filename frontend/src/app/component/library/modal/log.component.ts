import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";
import {LogService} from "../../../service/log.service";
import {LoadingState} from "../../../domain/common.model";
import {LogMessageDto, LogMessagePageDto} from "../../../domain/library.dto";
import {CommonModule, DatePipe} from "@angular/common";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";

@Component({
  standalone: true,
  imports: [TranslateModule, DatePipe, ErrorIndicatorComponent, LoadingIndicatorComponent, CommonModule],
  selector: 'pony-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss']
})
export class LogComponent implements OnInit {

  LoadingState = LoadingState;
  Level = LogMessageDto.Level;

  loadingState: LoadingState = LoadingState.LOADING;

  logs: LogMessageDto[] = [];
  page: LogMessagePageDto | undefined;

  @ViewChild('scroller') containerElement!: ElementRef;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly logService: LogService
  ) {
  }

  ngOnInit(): void {
    this.loadPage();
  }

  private loadPage(pageIndex = 0, pageSize = 30) {
    this.logService.getLog(undefined, undefined, undefined, pageIndex, pageSize).subscribe({
      next: page => {
        this.logs = page.logMessages;
        this.page = page;
        this.containerElement.nativeElement.scrollTop = 0;
        this.loadingState = LoadingState.LOADED;
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  loadPreviousPage() {
    if (this.page && this.page.pageIndex > 0) {
      this.loadPage(this.page.pageIndex - 1);
    }
  }

  loadNextPage() {
    if (this.page && this.page.pageIndex < (this.page.totalPages - 1)) {
      this.loadPage(this.page.pageIndex + 1);
    }
  }
}
