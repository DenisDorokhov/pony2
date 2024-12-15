CREATE TABLE playlist (

    id CHAR(36) PRIMARY KEY,

    creation_date TIMESTAMP(9) NOT NULL,
    update_date TIMESTAMP(9),

    name VARCHAR (255) NOT NULL,
    type VARCHAR (255) NOT NULL,
    pony_user_id CHAR(36) NOT NULL,

    FOREIGN KEY (pony_user_id) REFERENCES pony_user (id)
);

CREATE TABLE playlist_song (

    id CHAR(36) PRIMARY KEY,

    sort INT NOT NULL,

    playlist_id CHAR(36) NOT NULL,
    song_id CHAR(36) NOT NULL,

    FOREIGN KEY (playlist_id) REFERENCES playlist (id),
    FOREIGN KEY (song_id) REFERENCES song (id)
);
