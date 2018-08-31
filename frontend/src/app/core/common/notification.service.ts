import {Injectable} from '@angular/core';
import * as Noty from "noty";

export enum NotificationType {
  SUCCESS,
  WARNING,
  ERROR,
}

export interface NotificationOptions {
  text: string;
  type: NotificationType,
  sticky?: boolean,
}

@Injectable()
export class NotificationService {
  
  showNotification(notificationOptions: NotificationOptions) {
    let notyType: string;
    switch (notificationOptions.type) {
      case NotificationType.SUCCESS:
        notyType = 'success';
        break;
      case NotificationType.WARNING:
        notyType = 'warning';
        break;
      case NotificationType.ERROR:
        notyType = 'error';
        break;
      default:
        throw new Error('Unsupported notification type.');
    }
    new Noty({
      text: notificationOptions.text,
      type: 'error',
      theme: 'bootstrap-v3',
      closeWith: ['click', 'button'],
      timeout: notificationOptions.sticky ? false : 3500,
      progressBar: false,
    }).show();
  }
}