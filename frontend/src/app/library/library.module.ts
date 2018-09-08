import {NgModule} from '@angular/core';
import {AlertModule, BsDropdownModule, ModalModule} from 'ngx-bootstrap';
import {SharedModule} from '../shared/shared.module';
import {AlbumListComponent} from './album-list.component';
import {AlbumComponent} from './album.component';
import {ArtistListComponent} from './artist-list.component';
import {ArtistComponent} from './artist.component';
import {CurrentUserComponent} from './current-user.component';
import {LibraryRoutingModule} from './library-routing.module';
import {LibraryComponent} from './library.component';
import {LogComponent} from './log.component';
import {PlayerComponent} from './player.component';
import {ScanningComponent} from './scanning.component';
import {SettingsComponent} from './settings.component';
import {SongListComponent} from './song-list.component';
import {SongComponent} from './song.component';
import {ToolbarComponent} from './toolbar.component';
import {UserListComponent} from './user-list.component';

@NgModule({
  imports: [
    LibraryRoutingModule,
    SharedModule,
    AlertModule.forRoot(),
    BsDropdownModule.forRoot(),
    ModalModule.forRoot(),
  ],
  declarations: [
    LibraryComponent,
    ToolbarComponent,
    ArtistListComponent,
    ArtistComponent,
    AlbumListComponent,
    AlbumComponent,
    SongListComponent,
    SongComponent,
    PlayerComponent,
    CurrentUserComponent,
    LogComponent,
    ScanningComponent,
    SettingsComponent,
    UserListComponent,
  ],
  entryComponents: [
    CurrentUserComponent,
    LogComponent,
    ScanningComponent,
    SettingsComponent,
    UserListComponent,
  ]
})
export class LibraryModule {
}
