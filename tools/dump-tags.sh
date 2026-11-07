#!/usr/bin/env bash
# Liest die Tags aller Audiodateien in einem Ordner (rekursiv) mit ffprobe aus
# und schreibt pro Datei eine Zeile JSON. Ändert keine Dateien.
#
# Aufruf:  ./dump-tags.sh "/pfad/zu/Gabriel Burns" > gabriel-burns-tags.jsonl
# Voraussetzung: sudo apt install ffmpeg jq
#
# Zeilen mit "kind":"audio" beschreiben eine Audiodatei, Zeilen mit "kind":"image"
# eine Bilddatei (mögliches Cover). Ältere Dumps ohne "kind" enthalten nur Audiodateien.
#
# Für MP3 zusätzlich:
#   id3v2: "2.3" / "2.4" / ... oder null, wenn am Dateianfang kein ID3v2-Tag steht
#   id3v1: true, wenn am Dateiende ein ID3v1-Tag ("TAG") steht
# ffprobe führt ID3v1 und ID3v2 zusammen; Media3 (und damit die App) liest bei MP3 nur ID3v2.
set -euo pipefail

ROOT="${1:?Bitte Ordner angeben}"

# Prints the ID3v2 version ("2.3", "2.4", ...) of a file, or nothing.
id3v2_version() {
  local hex
  hex=$(head -c 4 -- "$1" | od -An -tx1 | tr -d ' \n')
  if [[ "${hex:0:6}" == "494433" && ${#hex} -eq 8 ]]; then
    echo "2.$((16#${hex:6:2}))"
  fi
}

has_id3v1() {
  local size
  size=$(stat -c %s -- "$1")
  if ((size >= 128)) && [[ "$(tail -c 128 -- "$1" | head -c 3)" == "TAG" ]]; then
    echo true
  else
    echo false
  fi
}

find "$ROOT" -type f \( -iname '*.mp3' -o -iname '*.m4a' -o -iname '*.m4b' -o -iname '*.flac' \
  -o -iname '*.ogg' -o -iname '*.opus' -o -iname '*.mka' -o -iname '*.wma' \
  -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) -print0 | sort -z |
while IFS= read -r -d '' f; do
  rel="${f#"$ROOT"/}"
  case "${f,,}" in
    *.jpg | *.jpeg | *.png | *.webp)
      jq -cn --arg path "$rel" --argjson size "$(stat -c %s -- "$f")" \
        '{kind: "image", path: $path, size: $size}'
      continue
      ;;
  esac

  id3v2=null
  id3v1=null
  if [[ "${f,,}" == *.mp3 ]]; then
    v=$(id3v2_version "$f")
    [[ -n "$v" ]] && id3v2="\"$v\""
    id3v1=$(has_id3v1 "$f")
  fi

  ffprobe -v quiet -print_format json -show_format -show_chapters "$f" |
    jq -c --arg path "$rel" --argjson id3v2 "$id3v2" --argjson id3v1 "$id3v1" '{
      kind: "audio",
      path: $path,
      duration: (.format.duration // null),
      id3v2: $id3v2,
      id3v1: $id3v1,
      tags: (.format.tags // {}),
      chapters: [(.chapters // [])[] | {start: .start_time, title: (.tags.title // null)}]
    }'
done
