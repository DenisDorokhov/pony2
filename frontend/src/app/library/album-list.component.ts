import {Component} from '@angular/core';
import {LoadingState} from '../core/common/loading-state';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-album-list',
  templateUrl: './album-list.component.html',
  styleUrls: ['./album-list.component.scss']
})
export class AlbumListComponent {

  LoadingState = LoadingState;

  loadingState = LoadingState.EMPTY;

  constructor(private libraryService: LibraryService) {
  }
}
