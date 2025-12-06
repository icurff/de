#!/bin/bash

VIDEO="$1"

# Check if ffprobe is available
if ! command -v ffprobe &> /dev/null; then
    echo "ERROR: ffprobe not found" >&2
    exit 1
fi

# Check if video file exists
if [ ! -f "$VIDEO" ]; then
    echo "ERROR: Video file not found: $VIDEO" >&2
    exit 1
fi

# Get video resolution
ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 "$VIDEO"