import {Artist} from './artist.model';

export class Album {

  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  year: number;
  artwork: number;
  artist: Artist;

  constructor(partial?: Partial<Album>) {
    Object.assign(this, partial);
  }
}
