import {Album} from './album.model';

export class Song {

  id: number;
  creationDate: Date;
  updateDate: Date;
  mimeType: string;
  size: number;
  duration: number;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number;
  trackNumber: number;
  name: string;
  artistName: string;
  album: Album;
  genre: number;

  constructor(partial?: Partial<Song>) {
    Object.assign(this, partial);
  }
}
