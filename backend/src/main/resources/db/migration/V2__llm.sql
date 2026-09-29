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
