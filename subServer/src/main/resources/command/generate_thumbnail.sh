#!/bin/bash

INPUT="$1"
OUTPUT="$2"
TIMESTAMP="$3"

if [ -z "$TIMESTAMP" ]; then
  TIMESTAMP="1"
fi

mkdir -p "$(dirname "$OUTPUT")"

ffmpeg -y -ss "$TIMESTAMP" -i "$INPUT" -frames:v 1 -q:v 2 "$OUTPUT"

if [ $? -eq 0 ]; then
  echo "Thumbnail created at $OUTPUT"
else
  echo "Failed to create thumbnail" >&2
  exit 1
fi



