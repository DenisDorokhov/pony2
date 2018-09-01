import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {Song} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';
import {PlaybackService, PlaybackState} from '../core/library/playback.service';

@Component({
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy {
  
  PlaybackState = PlaybackState;

  @Input() song: Song;
  @Input() showArtist = false;

  selected = false;
  duration: string;
  playbackState: PlaybackState | undefined;

  private selectedSongSubscription: Subscription;
  private playbackEventSubscription: Subscription;

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService
  ) {
  }

  ngOnInit(): void {
    this.selectedSongSubscription = this.libraryService.observeSelectedSong()
      .subscribe(song => {
        this.selected = song && song.id === this.song.id;
      });
    this.playbackEventSubscription = this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.id === this.song.id) {
          this.playbackState = playbackEvent.state;
        } else {
          this.playbackState = undefined;
        }
      });
    this.duration = this.song.durationInMinutes;
  }

  ngOnDestroy(): void {
    this.selectedSongSubscription.unsubscribe();
    this.playbackEventSubscription.unsubscribe();
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  play() {
    this.libraryService.requestSongPlayback(this.song);
  }
}
