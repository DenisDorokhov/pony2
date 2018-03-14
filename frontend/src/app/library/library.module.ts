import {NgModule} from '@angular/core';
import {AlertModule, BsDropdownModule} from 'ngx-bootstrap';
import {SharedModule} from '../shared/shared.module';
import {AlbumListComponent} from './album-list.component';
import {ArtistListComponent} from './artist-list.component';
import {LibraryRoutingModule} from './library-routing.module';
import {LibraryComponent} from './library.component';
import {ToolbarComponent} from './toolbar.component';

@NgModule({
  imports: [
    LibraryRoutingModule,
    SharedModule,
    AlertModule.forRoot(),
    BsDropdownModule.forRoot()
  ],
  declarations: [
    LibraryComponent,
    ToolbarComponent,
    ArtistListComponent,
    AlbumListComponent,
  ]
})
export class LibraryModule {
}
