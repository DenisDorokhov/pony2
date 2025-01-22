import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {
  NgbActiveModal,
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle
} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {LogService} from '../../../service/log.service';
import {LoadingState} from '../../../domain/common.model';
import {LogMessageDto, LogMessagePageDto} from '../../../domain/library.dto';
import {CommonModule, DatePipe} from '@angular/common';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import Level = LogMessageDto.Level;

class LogWithException implements LogMessageDto {

  id!: string;
  date!: string;
  level!: LogMessageDto.Level;
  pattern!: string;
  arguments!: string[];
  text!: string;
  exception?: string;

  constructor(log: LogMessageDto) {
    this.id = log.id;
    this.date = log.date;
    this.level = log.level;
    this.pattern = log.pattern;
    this.arguments = log.arguments;
    this.text = log.text;
    this.exception = LogMessageDto.extractException(log);
  }
}

@Component({
  standalone: true,
  imports: [TranslateModule, DatePipe, ErrorIndicatorComponent, LoadingIndicatorComponent, CommonModule, FormsModule, ReactiveFormsModule, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownButtonItem, NgbDropdownItem, NgbDropdown],
  selector: 'pony-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss']
})
export class LogComponent implements OnInit {

  readonly LoadingState = LoadingState;
  readonly Level = LogMessageDto.Level;

  loadingState: LoadingState = LoadingState.LOADING;

  readonly logLevels: LogMessageDto.Level[] = [Level.INFO, Level.WARN, Level.ERROR];

  logs: LogWithException[] = [];
  page: LogMessagePageDto | undefined;
  minLevel = LogMessageDto.Level.INFO;

  @ViewChild('scroller') containerElement!: ElementRef;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly logService: LogService,
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  private loadPage(pageIndex = 0, pageSize = 30) {
    this.logService.getLog(
      this.minLevel,
      pageIndex, pageSize
    ).subscribe({
      next: page => {
        this.logs = page.logMessages.map(next => new LogWithException(next));
        this.page = page;
        this.containerElement.nativeElement.scrollTop = 0;
        this.loadingState = page.totalPages > 0 ? LoadingState.LOADED : LoadingState.EMPTY;
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

  applyFilter(level: LogMessageDto.Level) {
    this.minLevel = level;
    this.loadPage(0, this.page?.pageSize);
  }
}
