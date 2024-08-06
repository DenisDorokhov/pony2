import {TranslateService} from "@ngx-translate/core";

export function formatFileSize(bytes: number, translateService: TranslateService) {
  const gigabytes = bytes / 1_000_000_000;
  if (gigabytes >= 1) {
    return translateService.instant('library.scanStatistics.sizeGigabytes', {
      size: gigabytes.toFixed(2)
    });
  } else {
    const megabytes = bytes / 1_000_000;
    return translateService.instant('library.scanStatistics.sizeMegabytes', {
      size: megabytes.toFixed(2)
    });
  }
}

export function formatDuration(durationSeconds: number, translateService: TranslateService) {
  const days = Math.floor(durationSeconds / (3600*24));
  const hours = Math.floor(durationSeconds % (3600*24) / 3600);
  const minutes = Math.floor(durationSeconds % 3600 / 60);
  const seconds = Math.floor(durationSeconds % 60);
  const parts: string[] = [];
  if (days) {
    parts.push(translateService.instant('shared.duration.days', {value: days}));
  }
  if (hours) {
    parts.push(translateService.instant('shared.duration.hours', {value: hours}));
  }
  if (minutes) {
    parts.push(translateService.instant('shared.duration.minutes', {value: minutes}));
  }
  if (seconds) {
    parts.push(translateService.instant('shared.duration.seconds', {value: seconds}));
  }
  return parts.join(', ');
}
