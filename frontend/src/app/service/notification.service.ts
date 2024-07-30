import {Injectable} from "@angular/core";
import {ToastrService} from "ngx-toastr";

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(
    private toastr: ToastrService
  ) {
  }

  success(title: string, text: string) {
    this.toastr.success(text, title);
  }

  error(title: string, text: string) {
    this.toastr.error(text, title);
  }
}
