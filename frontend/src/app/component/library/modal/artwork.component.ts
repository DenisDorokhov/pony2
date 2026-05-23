import {Component, inject, Input} from '@angular/core';
import {ImageLoaderComponent} from '../../common/image-loader.component';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    imports: [TranslateModule, ImageLoaderComponent],
    selector: 'pony-artwork',
    templateUrl: './artwork.component.html',
    styleUrls: ['./artwork.component.scss']
})
export class ArtworkComponent {

  readonly activeModal = inject(NgbActiveModal);

  @Input()
  url: string | undefined;
}
