import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import * as Logger from 'js-logger';
import {Subscription} from 'rxjs';
import {LibraryScanService} from '../core/library/library-scan.service';
import {ScanStatisticsDto} from '../core/library/library.dto';
import {AuthenticationService} from '../core/user/authentication.service';

@Component({
  selector: 'pony-library',
  templateUrl: './library.component.html',
  styleUrls: ['./library.component.scss']
})
export class LibraryComponent implements OnInit, OnDestroy {

  private loggedOutSubscription: Subscription;
  private scanStatisticsSubscription: Subscription;
  
  scanStatistics: ScanStatisticsDto | undefined;
  size: string | undefined;

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private libraryScanService: LibraryScanService,
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
  }

  ngOnDestroy(): void {
    this.loggedOutSubscription.unsubscribe();
    this.scanStatisticsSubscription.unsubscribe();
  }
  
  private calculateSize(): string {
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
