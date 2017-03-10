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

  roles LONGVARCHAR NOT NULL,

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

  type TINYINT NOT NULL,

  code VARCHAR (255) NOT NULL,
  text LONGVARCHAR,
  details LONGVARCHAR,

  arguments LONGVARCHAR NOT NULL
);

CREATE INDEX index_log_message_date ON log_message (date);
CREATE INDEX index_log_message_type ON log_message (type);
CREATE INDEX index_log_message_date_type ON log_message (date, type);

CREATE TABLE config (

  id VARCHAR (255) NOT NULL,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  value LONGVARCHAR,

  PRIMARY KEY (id)
);

CREATE TABLE stored_file (

  id BIGINT IDENTITY,

  date TIMESTAMP NOT NULL,

  name VARCHAR (255) NOT NULL,
  mime_type VARCHAR (255) NOT NULL,
  checksum VARCHAR (255) NOT NULL,
  size BIGINT NOT NULL,
  tag VARCHAR (255),
  path VARCHAR (255) NOT NULL,
  meta_data LONGVARCHAR,

  UNIQUE (path),
  UNIQUE (tag, checksum)
);

CREATE INDEX index_stored_file_checksum ON stored_file (checksum);
CREATE INDEX index_stored_file_tag ON stored_file (tag);

CREATE TABLE genre (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_stored_file_id BIGINT,

  FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file (id)
);

CREATE INDEX index_genre_name ON genre(name);

CREATE TABLE artist (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),

  artwork_stored_file_id BIGINT,

  FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file (id)
);

CREATE INDEX index_artist_name ON artist(name);

CREATE TABLE album (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  name VARCHAR_IGNORECASE (255),
  year INT,

  artwork_stored_file_id BIGINT,

  artist_id BIGINT NOT NULL,

  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file (id)
);

CREATE INDEX index_album_name_artist_id ON album (name, artist_id);
CREATE INDEX index_album_artist_id_year_name ON album (artist_id, year, name);

CREATE TABLE song (

  id BIGINT IDENTITY,

  creation_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP,

  path VARCHAR_IGNORECASE (255) NOT NULL,

  format VARCHAR (255) NOT NULL,
  mime_type VARCHAR (255) NOT NULL,
  size BIGINT NOT NULL,

  duration INT NOT NULL,
  bit_rate BIGINT NOT NULL,

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

  artwork_stored_file_id BIGINT,

  album_id BIGINT NOT NULL,
  genre_id BIGINT NOT NULL,

  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (genre_id) REFERENCES genre (id),
  FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file (id),

  UNIQUE (path)
);

CREATE INDEX index_song_track_number_name ON song (disc_number, track_number, name);
