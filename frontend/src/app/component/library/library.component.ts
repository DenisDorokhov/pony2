import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {mergeMap, Subscription} from 'rxjs';
import {ScanStatisticsDto} from "../../domain/library.dto";
import {AuthenticationService} from "../../service/authentication.service";
import {LibraryScanService} from "../../service/library-scan.service";
import Logger from "js-logger";
import {CommonModule} from "@angular/common";
import {PlayerComponent} from "./player.component";
import {ToolbarComponent} from "./toolbar.component";
import {ArtistListComponent} from "./artist-list.component";
import {AlbumListComponent} from "./album-list.component";
import {LibraryService} from "../../service/library.service";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, PlayerComponent, ToolbarComponent, ArtistListComponent, AlbumListComponent],
  selector: 'pony-library',
  templateUrl: './library.component.html',
  styleUrls: ['./library.component.scss']
})
export class LibraryComponent implements OnInit, OnDestroy {

  private loggedOutSubscription: Subscription | undefined;
  private scanStatisticsSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

  scanStatistics: ScanStatisticsDto | undefined;
  size: string | undefined;

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private libraryScanService: LibraryScanService,
    private libraryService: LibraryService,
    private translateService: TranslateService
  ) {
  }

  ngOnInit(): void {
    this.loggedOutSubscription = this.authenticationService.observeLogout()
      .subscribe(user => {
        Logger.info(`User ${user.email} has been logged out.`);
        this.router.navigate(['/login'], {replaceUrl: true});
      });
    this.scanStatisticsSubscription = this.libraryScanService.observeScanStatistics()
      .subscribe(scanStatistics => {
        this.scanStatistics = scanStatistics;
        this.size = this.calculateSize();
      });
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest().pipe(
      mergeMap(() => this.libraryScanService.updateScanStatistics())
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.loggedOutSubscription?.unsubscribe();
    this.scanStatisticsSubscription?.unsubscribe();
    this.refreshRequestSubscription?.unsubscribe();
  }

  private calculateSize(): string | undefined {
    if (!this.scanStatistics) {
      return undefined;
    }
    const gigabytes = this.scanStatistics.songSize / 1_000_000_000;
    if (gigabytes >= 1) {
      return this.translateService.instant('library.scanStatistics.sizeGigabytes', {
        size: gigabytes.toFixed(2)
      });
    } else {
      const megabytes = this.scanStatistics.songSize / 1_000_000;
      return this.translateService.instant('library.scanStatistics.sizeMegabytes', {
        size: megabytes.toFixed(2)
      });
    }
  }
}
