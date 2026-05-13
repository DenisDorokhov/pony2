Pony - Music Streamer
=====================

Pony is a self-hosted music streamer solution with an iTunes-like web interface. 
It indexes your personal MP3 collection, builds a searchable library, and streams it to your browser or Symfonium app.

## Features

- 🎧 **iTunes-like UI** — familiar three-pane layout (artists, albums, tracks), playback queue, scrubbing, and album artwork.
- 📚 **Huge libraries supported** — designed and tested with terabyte-scale collections (hundreds of thousands of tracks).
- 🔎 **Full-text search** powered by Hibernate Search + Lucene.
- 🖼️ **Automatic artwork** extraction from tags and folders, with on-the-fly thumbnailing (JPEG / PNG / WebP).
- 🏷️ **Smart tag reading** via JAudioTagger (ID3v1 / ID3v2 / etc.).
- 🔁 **Incremental library scans** which means only changed files are re-processed.
- 👥 **Multi-user app** with role-based access and JWT authentication.
- 📱 **OpenSubsonic support** that works flawlessly with Symfonium app.
- 🐳 **Docker-ready** for easy deployment.

## Tech stack

### Backend
- **Java**
- **Spring Boot** (Web, Data JPA, Security, Validation)
- **Hibernate ORM** + **Hibernate Search 7** with **Lucene** backend
- **H2** database
- **Flyway** migrations
- **JAudioTagger** metadata extraction
- **JWT** token-based authentication

### Frontend
- **Angular**
- **Bootstrap**

### Infrastructure
- **Docker** containerized deployment
- **Gradle** multi-module build into a single executable JAR
