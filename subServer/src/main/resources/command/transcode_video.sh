#!/bin/bash

INPUT=$1
OUTPUT=$2
HEIGHT=$3
NAME=$4

ffmpeg -y -i "$INPUT" -vf "scale=-2:${HEIGHT}" \
-c:v libx264 -preset medium -crf 23 \
-c:a aac -b:a 128k \
-f hls \
-hls_time 10 \
-hls_playlist_type vod \
-hls_segment_filename "$OUTPUT/${NAME}_${HEIGHT}p_%06d.ts" \
"$OUTPUT/${NAME}_${HEIGHT}p.m3u8"

if [ $? -eq 0 ]; then
  echo "Done: $OUTPUT/${NAME}_${HEIGHT}p.m3u8"

  # Get the parent directory for the master playlist (one level up from resolution folder)
  MASTER_DIR=$(dirname "$OUTPUT")
  MASTER_PLAYLIST="$MASTER_DIR/master.m3u8"

  # Start master playlist
  echo "#EXTM3U" > "$MASTER_PLAYLIST"

  # Check which resolutions exist and add them to master playlist
  # Sort from highest to lowest resolution
  for res in 1080 720 480 360 240; do
    RES_DIR="$MASTER_DIR/$res"
    PLAYLIST_FILE="$RES_DIR/${NAME}_${res}p.m3u8"

    if [ -f "$PLAYLIST_FILE" ]; then
      # Calculate bandwidth based on resolution
      case $res in
        1080) BANDWIDTH=8000000 ;;
        720)  BANDWIDTH=5000000 ;;
        480)  BANDWIDTH=2500000 ;;
        360)  BANDWIDTH=1000000 ;;
        240)  BANDWIDTH=600000 ;;
        *)    BANDWIDTH=$((res * 5000)) ;;
      esac
      
      # Calculate width based on height (16:9 aspect ratio)
      WIDTH=$((res * 16 / 9))
      
      # Write master playlist entry
      echo "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=${BANDWIDTH},RESOLUTION=${WIDTH}x${res},NAME=\"${res}p\"" >> "$MASTER_PLAYLIST"
      echo "$res/${NAME}_${res}p.m3u8" >> "$MASTER_PLAYLIST"
    fi
  done

  echo "Master playlist updated: $MASTER_PLAYLIST"
else
  echo "Failed."
fi
