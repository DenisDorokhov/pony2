import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {NgbActiveModal, NgbInputDatepicker} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";
import {LogService} from "../../../service/log.service";
import {LoadingState} from "../../../domain/common.model";
import {LogMessageDto, LogMessagePageDto} from "../../../domain/library.dto";
import {CommonModule, DatePipe} from "@angular/common";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import Level = LogMessageDto.Level;

@Component({
  standalone: true,
  imports: [TranslateModule, DatePipe, ErrorIndicatorComponent, LoadingIndicatorComponent, CommonModule, NgbInputDatepicker, FormsModule, ReactiveFormsModule],
  selector: 'pony-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss']
})
export class LogComponent implements OnInit {

  LoadingState = LoadingState;
  Level = LogMessageDto.Level;

  loadingState: LoadingState = LoadingState.LOADING;

  logLevels: LogMessageDto.Level[] = [Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR];

  logs: LogMessageDto[] = [];
  page: LogMessagePageDto | undefined;

  form: FormGroup;

  @ViewChild('scroller') containerElement!: ElementRef;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly logService: LogService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.form = this.formBuilder.group({
      minLevel: LogMessageDto.Level.INFO,
      minDate: '',
      maxDate: '',
    });
  }

  ngOnInit(): void {
    this.loadPage();
  }

  private loadPage(pageIndex = 0, pageSize = 30) {
    this.logService.getLog(
      this.form.value.minLevel,
      this.normalizeFormDate(this.form.value.minDate), this.normalizeFormDate(this.form.value.maxDate),
      pageIndex, pageSize
    ).subscribe({
      next: page => {
        this.logs = page.logMessages;
        this.page = page;
        this.containerElement.nativeElement.scrollTop = 0;
        this.loadingState = page.totalPages > 0 ? LoadingState.LOADED : LoadingState.EMPTY;
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  private normalizeFormDate(date: any): Date | undefined {
    if (typeof date === 'string') {
      return undefined;
    }
    return date as Date;
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

  applyFilter() {
    this.loadPage(0, this.page?.pageSize);
  }
}
