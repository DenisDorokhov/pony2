CREATE TABLE installation (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  version VARCHAR (255) NOT NULL
);

CREATE TABLE user (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR (255) NOT NULL,

  email VARCHAR_IGNORECASE (255) NOT NULL,

  password TEXT NOT NULL,

  UNIQUE (email)
);

CREATE INDEX index_user_name ON user (name);
CREATE INDEX index_user_name_email ON user (name, email);

CREATE TABLE user_role (

  user_id CHAR(36) NOT NULL,

  value VARCHAR (255) NOT NULL,

  FOREIGN KEY (user_id) REFERENCES user (id),

  PRIMARY KEY (user_id, value)
);

CREATE TABLE log_message (

  id CHAR(36) PRIMARY KEY,

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

  id CHAR(36) PRIMARY KEY,

  date TIMESTAMP NOT NULL,
  scan_type VARCHAR (255) NOT NULL,
  target_paths TEXT NOT NULL,
  failed_paths TEXT NOT NULL,
  processed_audio_file_count INT NOT NULL,
  duration BIGINT NOT NULL,

  song_size BIGINT NOT NULL,
  artwork_size BIGINT NOT NULL,

  genre_count INT NOT NULL,
  artist_count INT NOT NULL,
  album_count INT NOT NULL,
  song_count INT NOT NULL,
  artwork_count INT NOT NULL,

  created_artist_count INT NOT NULL,
  updated_artist_count INT NOT NULL,
  deleted_artist_count INT NOT NULL,

  created_album_count INT NOT NULL,
  updated_album_count INT NOT NULL,
  deleted_album_count INT NOT NULL,

  created_genre_count INT NOT NULL,
  updated_genre_count INT NOT NULL,
  deleted_genre_count INT NOT NULL,

  created_song_count INT NOT NULL,
  updated_song_count INT NOT NULL,
  deleted_song_count INT NOT NULL,

  created_artwork_count INT NOT NULL,
  deleted_artwork_count INT NOT NULL
);

CREATE INDEX index_scan_result_date ON scan_result (date);

CREATE TABLE scan_job (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  scan_type VARCHAR (255) NOT NULL,
  status VARCHAR (255) NOT NULL,

  log_message_id CHAR(36),
  scan_result_id CHAR(36),

  FOREIGN KEY (log_message_id) REFERENCES log_message (id),
  FOREIGN KEY (scan_result_id) REFERENCES scan_result (id)
);

CREATE INDEX index_scan_job_status ON scan_job (status);

CREATE TABLE artwork (

  id CHAR(36) PRIMARY KEY,

  date TIMESTAMP NOT NULL,

  mime_type VARCHAR (255) NOT NULL,
  checksum VARCHAR (255) NOT NULL,
  large_image_size BIGINT NOT NULL,
  large_image_path VARCHAR (255) NOT NULL,
  small_image_size BIGINT NOT NULL,
  small_image_path VARCHAR (255) NOT NULL,
  source_uri VARCHAR (1000) NOT NULL,
  source_uri_scheme VARCHAR (255),

  UNIQUE (source_uri_scheme, checksum),
  UNIQUE (source_uri),
  UNIQUE (large_image_path),
  UNIQUE (small_image_path)
);

CREATE TABLE genre (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_id CHAR(36),

  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_genre_name ON genre(name);

CREATE TABLE artist (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_id CHAR(36),

  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_artist_name ON artist(name);

CREATE TABLE album (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),
  year INT,

  artwork_id CHAR(36),

  artist_id CHAR(36) NOT NULL,

  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (artwork_id) REFERENCES artwork (id)
);

CREATE INDEX index_album_name_artist_id ON album (name, artist_id);
CREATE INDEX index_album_artist_id_year_name ON album (artist_id, year, name);

CREATE TABLE song (

  id CHAR(36) PRIMARY KEY,

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

  artwork_id CHAR(36),

  album_id CHAR(36) NOT NULL,
  genre_id CHAR(36) NOT NULL,

  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (genre_id) REFERENCES genre (id),
  FOREIGN KEY (artwork_id) REFERENCES artwork (id),

  UNIQUE (path)
);

CREATE INDEX index_song_disc_number_track_number_name ON song (disc_number, track_number, name);
