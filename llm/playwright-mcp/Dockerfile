FROM mcr.microsoft.com/playwright/mcp:latest

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        xvfb \
        xauth \
    && rm -rf /var/lib/apt/lists/*

# Persistent Chrome profile.
# Important: node must be able to write here.
RUN mkdir -p /data/profile \
    && chown -R node:node /data

USER node

# Remove stale Chromium profile locks before starting Playwright.
ENTRYPOINT ["/bin/sh", "-c", "rm -f /data/profile/SingletonLock /data/profile/SingletonSocket /data/profile/SingletonCookie && exec xvfb-run -a -s '-screen 0 1920x1080x24' node /app/cli.js \"$@\"", "--"]
