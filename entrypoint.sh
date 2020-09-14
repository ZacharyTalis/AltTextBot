#!/usr/bin/env bash
set -e

source /app/.env
java --enable-preview -jar /app/AltTextBot-1.0.jar
