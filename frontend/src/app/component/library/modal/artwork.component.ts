import {Component, Input} from '@angular/core';
import {ImageLoaderComponent} from '../../common/image-loader.component';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
  standalone: true,
  imports: [TranslateModule, ImageLoaderComponent],
  selector: 'pony-artwork',
  templateUrl: './artwork.component.html',
  styleUrls: ['./artwork.component.scss']
})
export class ArtworkComponent {

  @Input()
  url: string | undefined;

  constructor(
    public readonly activeModal: NgbActiveModal,
  ) {
  }
}
