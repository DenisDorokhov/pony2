import {InstallationStatusDto} from '../domain/installation.dto';

export function shouldShowNewIndicator(date: Date | undefined, installationStatus: InstallationStatusDto | undefined) {
  if (!date) {
    return false;
  }
  if (!installationStatus?.initialScanDate) {
    return false;
  }
  const threeDaysAgo = new Date();
  threeDaysAgo.setDate(threeDaysAgo.getDate() - 2);
  return new Date(installationStatus!.initialScanDate).getTime() < date.getTime() && date.getTime() >= threeDaysAgo.getTime();
}
