#!/usr/bin/env zsh
set -Ee

if [ $# -lt 1 ]; then
  {
    echo "Missing version argument"
    echo "---"
    echo "Usage: ./publish.sh <VERSION_NAME>"
  } >&2

  exit 1
fi

version="$1"

record() {
  echo "+ $*"
  "$@"
}


record ./gradlew assemble

record docker build -f ./Dockerfile -t alt-text-bot --build-arg version="$version" ./build/libs
record docker tag alt-text-bot glossawy/alt-text-bot:"$version"
record docker tag alt-text-bot glossawy/alt-text-bot:current

record docker push glossawy/alt-text-bot:"$version"
record docker push glossawy/alt-text-bot:current
