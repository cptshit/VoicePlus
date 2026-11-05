#!/usr/bin/env bash
# Liest die Tags aller Audiodateien in einem Ordner (rekursiv) mit ffprobe aus
# und schreibt pro Datei eine Zeile JSON. Ändert keine Dateien.
#
# Aufruf:  ./dump-tags.sh "/pfad/zu/Gabriel Burns" > gabriel-burns-tags.jsonl
# Voraussetzung: sudo apt install ffmpeg jq
set -euo pipefail

ROOT="${1:?Bitte Ordner angeben}"

find "$ROOT" -type f \( -iname '*.mp3' -o -iname '*.m4a' -o -iname '*.m4b' -o -iname '*.flac' \
  -o -iname '*.ogg' -o -iname '*.opus' -o -iname '*.mka' -o -iname '*.wma' \) -print0 | sort -z |
while IFS= read -r -d '' f; do
  rel="${f#"$ROOT"/}"
  ffprobe -v quiet -print_format json -show_format -show_chapters "$f" |
    jq -c --arg path "$rel" '{
      path: $path,
      duration: (.format.duration // null),
      tags: (.format.tags // {}),
      chapters: [(.chapters // [])[] | {start: .start_time, title: (.tags.title // null)}]
    }'
done
