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
- ✨ **AI-assisted discovery** — enrich your library with Spotify and MusicBrainz data using a configurable LLM endpoint and model.
- 🔥 **Spotify top tracks** — match popular tracks to songs in your collection, highlight them in the library and player, and show them alongside an artist's favorites.
- 🌍 **Artist countries** — country flags and filtering by country of origin.
- 💿 **Album release metadata** — LP, EP, single, live, and compilation badges, with original release dates used to order albums within the same year.
- 👥 **Multi-user app** with role-based access and JWT authentication.
- 📱 **OpenSubsonic support** that works flawlessly with Symfonium app.
- 🐳 **Docker-ready** for easy deployment.

## Tech stack

### Backend
- **Java**
- **Spring Boot 4** (Web MVC, Data JPA, Security, Validation)
- **Spring AI** for LLM integration through OpenAI-compatible APIs and MCP tools
- **Hibernate ORM** + **Hibernate Search** with **Lucene** backend
- **H2** database
- **Flyway** migrations
- **JAudioTagger** metadata extraction
- **JWT** token-based authentication

### Frontend
- **Angular**
- **Bootstrap**

### Infrastructure
- **Docker Compose** containerized deployment
- **Playwright MCP** with **Chromium** for browser-based discovery
- **Squid** security proxy to block discovery browser access to private and local networks and allow only HTTP/HTTPS traffic
- **Gradle** multi-module build into a single executable JAR
