CREATE TABLE discovery_cache (

  id CHAR(36) PRIMARY KEY,

  region VARCHAR (255) NOT NULL,
  "key" VARCHAR (255) NOT NULL,
  "version" INT NOT NULL,

  "value" LONGVARCHAR,

  creation_date TIMESTAMP(9) NOT NULL,
  expiration_date TIMESTAMP(9) NOT NULL
);

CREATE UNIQUE INDEX index_discovery_cache_region_key_version ON discovery_cache (region, "key", "version");
CREATE INDEX index_discovery_cache_expiration_date ON discovery_cache (expiration_date);

CREATE TABLE discovery_job (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  discovery_type VARCHAR (255) NOT NULL,
  status VARCHAR (255) NOT NULL,

  log_message_id CHAR(36),
  genre_id CHAR(36),
  artist_id CHAR(36),
  album_id CHAR(36),
  song_id CHAR(36),

  FOREIGN KEY (log_message_id) REFERENCES log_message (id),
  FOREIGN KEY (genre_id) REFERENCES genre (id),
  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (song_id) REFERENCES song (id),

  UNIQUE (log_message_id),

  CONSTRAINT check_discovery_job_target CHECK (
    (CASE WHEN genre_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN artist_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN album_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN song_id IS NULL THEN 0 ELSE 1 END) <= 1
  )
);

CREATE INDEX index_discovery_job_status ON discovery_job (status);

CREATE TABLE discovery_task (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  status VARCHAR (255) NOT NULL,
  type VARCHAR (255) NOT NULL,
  result LONGVARCHAR,
  raw_request LONGVARCHAR,
  raw_result LONGVARCHAR,

  discovery_job_id CHAR(36) NOT NULL,
  genre_id CHAR(36),
  artist_id CHAR(36),
  album_id CHAR(36),
  song_id CHAR(36),

  FOREIGN KEY (discovery_job_id) REFERENCES discovery_job (id),
  FOREIGN KEY (genre_id) REFERENCES genre (id),
  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (song_id) REFERENCES song (id),

  CONSTRAINT check_discovery_task_target CHECK (
    (CASE WHEN genre_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN artist_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN album_id IS NULL THEN 0 ELSE 1 END) +
    (CASE WHEN song_id IS NULL THEN 0 ELSE 1 END) <= 1
  )
);

CREATE INDEX index_discovery_task_update_date ON discovery_task (update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_type_update_date ON discovery_task (type, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_status_update_date ON discovery_task (status DESC, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_status_type_update_date ON discovery_task (status, type, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_status_result_update_date ON discovery_task (status, result, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_status_type_result_update_date ON discovery_task (status, type, result, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_job_type_update_date ON discovery_task (discovery_job_id, type, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_job_status_update_date ON discovery_task (discovery_job_id, status, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_job_status_type_update_date ON discovery_task (discovery_job_id, status, type, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_job_status_result_update_date ON discovery_task (discovery_job_id, status, result, update_date DESC NULLS LAST, id DESC);
CREATE INDEX index_discovery_task_job_status_type_result_update_date ON discovery_task (discovery_job_id, status, type, result, update_date DESC NULLS LAST, id DESC);

CREATE INDEX index_discovery_task_genre_status_type_creation_date ON discovery_task (genre_id, status, type, creation_date DESC, id DESC);
CREATE INDEX index_discovery_task_artist_status_type_creation_date ON discovery_task (artist_id, status, type, creation_date DESC, id DESC);
CREATE INDEX index_discovery_task_album_status_type_creation_date ON discovery_task (album_id, status, type, creation_date DESC, id DESC);
CREATE INDEX index_discovery_task_song_status_type_creation_date ON discovery_task (song_id, status, type, creation_date DESC, id DESC);

CREATE TABLE artist_discovery_index (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  artist_id CHAR(36) NOT NULL,
  spotify_artist_id VARCHAR (255),
  music_brainz_artist_id VARCHAR (255),
  country_code VARCHAR (255),

  FOREIGN KEY (artist_id) REFERENCES artist (id),

  UNIQUE (artist_id)
);

CREATE INDEX index_artist_discovery_index_spotify_artist_id ON artist_discovery_index (spotify_artist_id);
CREATE INDEX index_artist_discovery_index_music_brainz_artist_id ON artist_discovery_index (music_brainz_artist_id);
CREATE INDEX index_artist_discovery_index_country_code ON artist_discovery_index (country_code);

CREATE INDEX index_song_creation_date ON song (creation_date);
CREATE INDEX index_song_update_date_creation_date ON song (update_date, creation_date);
CREATE INDEX index_album_creation_date ON album (creation_date);
CREATE INDEX index_album_update_date_creation_date ON album (update_date, creation_date);
CREATE INDEX index_artwork_date ON artwork (date);
CREATE INDEX index_album_artist_id_name ON album (artist_id, name);

DROP INDEX index_user_email;
