CREATE TABLE llm_cache (

  id CHAR(36) PRIMARY KEY,

  region VARCHAR (255) NOT NULL,
  "key" VARCHAR (255) NOT NULL,
  "version" INT NOT NULL,

  "value" LONGVARCHAR,

  creation_date TIMESTAMP(9) NOT NULL,
  expiration_date TIMESTAMP(9) NOT NULL
);

CREATE UNIQUE INDEX index_llm_cache_region_key_version ON llm_cache (region, "key", "version");
CREATE INDEX index_llm_cache_expiration_date ON llm_cache (expiration_date);

CREATE TABLE discovery_result (

  id CHAR(36) PRIMARY KEY,

  date TIMESTAMP(9) NOT NULL,
  discovery_type VARCHAR (255) NOT NULL,

  completed_tasks_count BIGINT NOT NULL,
  failed_tasks_count BIGINT NOT NULL
);

CREATE INDEX index_discovery_result_date ON discovery_result (date desc);

CREATE TABLE discovery_job (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  discovery_type VARCHAR (255) NOT NULL,
  status VARCHAR (255) NOT NULL,
  parameter LONGVARCHAR,

  log_message_id CHAR(36),
  discovery_result_id CHAR(36),

  FOREIGN KEY (log_message_id) REFERENCES log_message (id),
  FOREIGN KEY (discovery_result_id) REFERENCES discovery_result (id),

  UNIQUE (log_message_id),
  UNIQUE (discovery_result_id)
);

CREATE INDEX index_discovery_job_status ON discovery_job (status);

CREATE TABLE discovery_task (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  status VARCHAR (255) NOT NULL,
  type VARCHAR (255) NOT NULL,
  argument LONGVARCHAR NOT NULL,
  result LONGVARCHAR,

  discovery_job_id CHAR(36) NOT NULL,

  FOREIGN KEY (discovery_job_id) REFERENCES discovery_job (id)
);

CREATE INDEX index_discovery_task_status ON discovery_task (status);
CREATE INDEX index_discovery_task_job_id_status ON discovery_task (discovery_job_id, status);

CREATE TABLE artist_discovery (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  artist_id CHAR(36) NOT NULL,
  discovery_job_id CHAR(36) NOT NULL,

  FOREIGN KEY (artist_id) REFERENCES artist (id),
  FOREIGN KEY (discovery_job_id) REFERENCES discovery_job (id),

  UNIQUE (artist_id, discovery_job_id)
);

CREATE INDEX index_artist_discovery_artist_id_creation_date ON artist_discovery (artist_id, creation_date desc);

CREATE TABLE artist_discovery_task (

  artist_discovery_id CHAR(36) NOT NULL,
  discovery_task_id CHAR(36) NOT NULL,

  FOREIGN KEY (artist_discovery_id) REFERENCES artist_discovery (id),
  FOREIGN KEY (discovery_task_id) REFERENCES discovery_task (id),

  PRIMARY KEY (artist_discovery_id, discovery_task_id),
  UNIQUE (discovery_task_id)
);

CREATE TABLE album_discovery (

  id CHAR(36) PRIMARY KEY,

  creation_date TIMESTAMP(9) NOT NULL,
  update_date TIMESTAMP(9),

  album_id CHAR(36) NOT NULL,
  discovery_job_id CHAR(36) NOT NULL,

  FOREIGN KEY (album_id) REFERENCES album (id),
  FOREIGN KEY (discovery_job_id) REFERENCES discovery_job (id),

  UNIQUE (album_id, discovery_job_id)
);

CREATE INDEX index_album_discovery_album_id_creation_date ON album_discovery (album_id, creation_date desc);

CREATE TABLE album_discovery_task (

  album_discovery_id CHAR(36) NOT NULL,
  discovery_task_id CHAR(36) NOT NULL,

  FOREIGN KEY (album_discovery_id) REFERENCES album_discovery (id),
  FOREIGN KEY (discovery_task_id) REFERENCES discovery_task (id),

  PRIMARY KEY (album_discovery_id, discovery_task_id),
  UNIQUE (discovery_task_id)
);
