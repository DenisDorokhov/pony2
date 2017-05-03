CREATE TABLE installation (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  version VARCHAR (255) NOT NULL
);

CREATE TABLE user (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR (255) NOT NULL,

  email VARCHAR_IGNORECASE (255) NOT NULL,

  password VARCHAR (255) NOT NULL,

  UNIQUE (email)
);

CREATE INDEX index_user_name ON user (name);
CREATE INDEX index_user_name_email ON user (name, email);

CREATE TABLE user_role (

  user_id BIGINT NOT NULL,

  value VARCHAR (255) NOT NULL,

  FOREIGN KEY (user_id) REFERENCES user (id),

  PRIMARY KEY (user_id, value)
);

CREATE TABLE log_message (

  id BIGINT IDENTITY,

  date TIMESTAMP NOT NULL,

  level TINYINT NOT NULL,

  pattern LONGVARCHAR NOT NULL,
  arguments LONGVARCHAR,

  text LONGVARCHAR NOT NULL
);

CREATE INDEX index_log_message_date ON log_message (date);
CREATE INDEX index_log_message_type ON log_message (level);
CREATE INDEX index_log_message_date_type ON log_message (date, level);

CREATE TABLE config (

  id VARCHAR (255) NOT NULL,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  value LONGVARCHAR,

  PRIMARY KEY (id)
);

CREATE TABLE scan_result (

  id BIGINT IDENTITY,

  date TIMESTAMP NOT NULL,

  scan_type VARCHAR (255) NOT NULL,
  
  target_paths TEXT NOT NULL,
  failed_paths TEXT NOT NULL,

  duration BIGINT NOT NULL,

  song_size BIGINT NOT NULL,
  artwork_size BIGINT NOT NULL,

  genre_count BIGINT NOT NULL,
  artist_count BIGINT NOT NULL,
  album_count BIGINT NOT NULL,
  song_count BIGINT NOT NULL,
  artwork_count BIGINT NOT NULL,

  processed_song_count BIGINT NOT NULL,

  created_artist_count BIGINT NOT NULL,
  updated_artist_count BIGINT NOT NULL,
  deleted_artist_count BIGINT NOT NULL,

  created_album_count BIGINT NOT NULL,
  updated_album_count BIGINT NOT NULL,
  deleted_album_count BIGINT NOT NULL,

  created_genre_count BIGINT NOT NULL,
  updated_genre_count BIGINT NOT NULL,
  deleted_genre_count BIGINT NOT NULL,

  created_song_count BIGINT NOT NULL,
  updated_song_count BIGINT NOT NULL,
  deleted_song_count BIGINT NOT NULL,

  created_artwork_count BIGINT NOT NULL,
  deleted_artwork_count BIGINT NOT NULL
);

CREATE INDEX index_scan_result_date ON scan_result (date);

CREATE TABLE scan_job (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  scan_type VARCHAR (255) NOT NULL,
  status VARCHAR (255) NOT NULL,

  log_message_id BIGINT,
  scan_result_id BIGINT,

  FOREIGN KEY (log_message_id) REFERENCES log_message (id),
  FOREIGN KEY (scan_result_id) REFERENCES scan_result (id)
);

CREATE INDEX index_scan_job_status ON scan_job (status);

CREATE TABLE artwork (

  id BIGINT IDENTITY,

  date TIMESTAMP NOT NULL,

  mime_type VARCHAR (255) NOT NULL,
  checksum VARCHAR (255) NOT NULL,
  large_image_size BIGINT NOT NULL,
  large_image_path VARCHAR (255) NOT NULL,
  small_image_size BIGINT NOT NULL,
  small_image_path VARCHAR (255) NOT NULL,
  source_uri VARCHAR (1000) NOT NULL,

  UNIQUE (large_image_path),
  UNIQUE (small_image_path),
  UNIQUE (source_uri)
);

CREATE INDEX index_artwork_source_uri ON artwork (source_uri);

CREATE TABLE genre (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_id BIGINT,

  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_genre_name ON genre(name);

CREATE TABLE artist (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_id BIGINT,

  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_artist_name ON artist(name);

CREATE TABLE album (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),
  year INT,

  artwork_id BIGINT,

  artist_id BIGINT NOT NULL,

  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_album_name_artist_id ON album (name, artist_id);
CREATE INDEX index_album_artist_id_year_name ON album (artist_id, year, name);

CREATE TABLE song (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  path VARCHAR_IGNORECASE (1000) NOT NULL,

  mime_type VARCHAR (255) NOT NULL,
  file_extension VARCHAR (255) NOT NULL,
  size BIGINT NOT NULL,

  duration BIGINT NOT NULL,
  bit_rate BIGINT NOT NULL,
  bit_rate_variable BOOL NOT NULL,

  disc_number INT,
  disc_count INT,

  track_number INT,
  track_count INT,

  name VARCHAR (255),
  genre_name VARCHAR (255),
  artist_name VARCHAR (255),
  album_artist_name VARCHAR (255),
  album_name VARCHAR (255),

  year INT,

  artwork_id BIGINT,

  album_id BIGINT NOT NULL,
  genre_id BIGINT NOT NULL,

  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (genre_id) REFERENCES genre (id),
  FOREIGN KEY (artwork_id) REFERENCES artwork (id),

  UNIQUE (path)
);

CREATE INDEX index_song_track_number_name ON song (disc_number, track_number, name);
