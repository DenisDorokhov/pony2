import {TranslateService} from "@ngx-translate/core";
import {DurationInput} from "@formatjs/intl-durationformat/lib/src/types";
import {DurationFormat} from "@formatjs/intl-durationformat";

export function renderFileSize(bytes: number, translateService: TranslateService) {
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

export function renderDuration(seconds: number, translateService: TranslateService) {
  const duration: DurationInput = {
    days: Math.floor(seconds / (3600*24)),
    hours: Math.floor(seconds % (3600*24) / 3600),
    minutes: Math.floor(seconds % 3600 / 60),
    seconds: Math.floor(seconds % 60),
  };
  return new DurationFormat(translateService.currentLang, { style: "narrow" }).format(duration);
}
