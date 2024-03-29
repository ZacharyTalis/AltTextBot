#!/usr/bin/env zsh
set -Eeuo pipefail

show-help() {
  echo "Missing version argument"
  echo "---"
  echo "Usage: ./publish.sh <arguments>"
  echo "Required Arguments:"
  echo "  -a|--author  <author >  : Docker author tag, e.g. 'glossawy' in glossawy/image-name:3.2.1"
  echo "  -t|--tag     <app-tag>  : Local docker tag for app, e.g. 'image-name' in glossawy/image-name:3.2.1"
  echo "  -v|--version <version>  : App version, e.g. '3.2.1' in glossawy/image-name:3.2.1"
  echo
  echo "Options:"
  echo "  -h|--help : print this help text and exit"
}

[[ $# -eq 0 ]] && show-help && exit 1

error-unknown-flag() {
  {
    echo "error: $1 is not a valid flag"
    show-help
  } &>2

  exit 1
}

error-missing-argument() {
  {
    echo "error: missing value for $1"
    show-help
  } &>2

  exit 2
}

author=false
local_tag=false
version=false

while [[ $# -gt 0 ]]; do
  flag="$1"

  case "$flag" in
  -a | --author)
    author="$2"
    shift
    shift
    ;;
  -t | --tag)
    local_tag="$2"
    shift
    shift
    ;;
  -v | --version)
    version="$2"
    shift
    shift
    ;;
  -h | --help)
    show-help
    exit 0
    ;;
  *)
    error-unknown-flag "$flag"
    ;;
  esac
done

[[ "$author" == false ]] && error-missing-argument '--author'
[[ "$version" == false ]] && error-missing-argument '--version'
[[ "$local_tag" == false ]] && error-missing-argument '--name'

record() {
  echo "+ $*"
  "$@"
}

docker_hub_tag="${author}/${local_tag}"

echo "App        : $local_tag"
echo "Author     : $author"
echo "Docker Tag : $docker_hub_tag"
echo "Version    : $version"
echo

record ./gradlew --console verbose assemble

record docker build -f ./Dockerfile -t "$local_tag" --build-arg version="$version" ./build/libs

record docker tag "$local_tag" "$docker_hub_tag":"$version"
record docker tag "$local_tag" "$docker_hub_tag":current

record docker buildx build --platform linux/arm64,linux/amd64 --push -f Dockerfile -t "$docker_hub_tag":"$version" --build-arg version="$version" ./build/libs
record docker buildx build --platform linux/arm64,linux/amd64 --push -f Dockerfile -t "$docker_hub_tag":current --build-arg version="$version" ./build/libs
