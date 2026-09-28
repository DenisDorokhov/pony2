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

ENTRYPOINT ["xvfb-run", "-a", "-s", "-screen 0 1920x1080x24", "node", "/app/cli.js"]
