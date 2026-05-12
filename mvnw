#!/usr/bin/env sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MAVEN_VERSION="3.9.15"
MAVEN_DIR="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
ARCHIVE="${TMPDIR:-/tmp}/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  echo "Downloading Apache Maven $MAVEN_VERSION..."
  if [ -f "$ARCHIVE" ]; then
    curl -L -C - "$MAVEN_URL" -o "$ARCHIVE"
  else
    curl -L "$MAVEN_URL" -o "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MAVEN_DIR/bin/mvn" "$@"
