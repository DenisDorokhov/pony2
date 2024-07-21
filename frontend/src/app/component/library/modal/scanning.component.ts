import {Component, OnInit} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";
import {LibraryScanService} from "../../../service/library-scan.service";
import {ScanJobProgressDto} from "../../../domain/library.dto";
import {LoadingState} from "../../../domain/common.model";
import Logger from "js-logger";

@Component({
  standalone: true,
  imports: [TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent],
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent implements OnInit {

  LoadingState = LoadingState;

  scanJobProgressLoadingState = LoadingState.LOADING;
  scanJobListLoadingState = LoadingState.LOADING;

  private scanJobProgress: ScanJobProgressDto | undefined;

  constructor(
    private libraryScanService: LibraryScanService,
    public readonly activeModal: NgbActiveModal
  ) {}

  ngOnInit(): void {
    this.scanJobProgressLoadingState = LoadingState.LOADING;
    this.scanJobListLoadingState = LoadingState.LOADING;
    this.libraryScanService.observeScanJobProgress().subscribe({
      next: scanJobProgress => {
        this.scanJobProgress = scanJobProgress;
        this.scanJobProgressLoadingState = LoadingState.LOADED;
      },
      error: error => {
        this.scanJobProgressLoadingState = LoadingState.ERROR;
        Logger.error(`Could not load scan job progress: "${error.message}".`);
      }
    });
    this.libraryScanService.updateScanJobProgress().subscribe();
  }
}
