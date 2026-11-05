#!/usr/bin/env bash
# Creates tiny audio files carrying the tags typically found on German audio drama
# ("Hörspiel") rips: track/disc numbers, album artist, grouping, year, movement name.
#
# ffmpeg cannot write every ID3 frame we care about (GRP1, MVNM, MVIN), so the ID3v2 tags
# are assembled by the embedded Python script and prepended to an untagged MP3 stream.
set -euo pipefail
cd "$(dirname "$0")"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

silence=(-f lavfi -i anullsrc=r=22050:cl=mono -t 1)

ffmpeg -v error -y "${silence[@]}" -c:a libmp3lame -b:a 32k -id3v2_version 0 -write_xing 0 "$tmp/raw.mp3"

python3 -I - "$tmp/raw.mp3" <<'EOF'
import sys

def syncsafe(n):
    return bytes([(n >> 21) & 0x7F, (n >> 14) & 0x7F, (n >> 7) & 0x7F, n & 0x7F])

def text_frame(frame_id, value, version, description=None):
    if version == 4:
        encoding, encode = b"\x03", lambda s: s.encode("utf-8")
        terminator = b"\x00"
    else:
        encoding, encode = b"\x01", lambda s: s.encode("utf-16")  # with BOM
        terminator = b"\x00\x00"
    body = encoding
    if description is not None:
        body += encode(description) + terminator
    body += encode(value)
    size = syncsafe(len(body)) if version == 4 else len(body).to_bytes(4, "big")
    return frame_id.encode("ascii") + size + b"\x00\x00" + body

def tag(frames, version):
    payload = b"".join(text_frame(*f, version=version) for f in frames)
    return b"ID3" + bytes([version, 0, 0]) + syncsafe(len(payload)) + payload

raw = open(sys.argv[1], "rb").read()

v24 = [
    ("TIT2", "Kapitel 03"),
    ("TPE1", "Gabriel Burns"),
    ("TALB", "(41) Verehrung"),
    ("TPE2", "Gabriel Burns"),
    ("TRCK", "03/06"),
    ("TPOS", "2/4"),
    ("TIT1", "Work Title"),
    ("GRP1", "Gabriel Burns Grouping"),
    ("TDRC", "2014-08-01"),
    ("MVNM", "Gabriel Burns"),
    ("MVIN", "41"),
]
v23 = [
    ("TIT2", "Kapitel 01"),
    ("TPE1", "Gabriel Burns"),
    ("TALB", "(01) Der Flüsterer"),
    ("TRCK", "7"),
    ("TPOS", "1"),
    ("TIT1", "Gabriel Burns"),
    ("TYER", "2003"),
    ("TCON", "Hörspiel"),
]
open("hoerspiel_tags_id3v24.mp3", "wb").write(tag(v24, 4) + raw)
open("hoerspiel_tags_id3v23.mp3", "wb").write(tag(v23, 3) + raw)
EOF

ffmpeg -v error -y "${silence[@]}" -c:a aac -b:a 32k \
  -metadata title="Kapitel 03" -metadata artist="Gabriel Burns" -metadata album="(41) Verehrung" \
  -metadata album_artist="Gabriel Burns" -metadata track="3/6" -metadata disc="2/4" \
  -metadata grouping="Gabriel Burns" -metadata date="2014" \
  hoerspiel_tags.m4a

ffmpeg -v error -y "${silence[@]}" -c:a flac \
  -metadata TITLE="Kapitel 03" -metadata ARTIST="Gabriel Burns" -metadata ALBUM="(41) Verehrung" \
  -metadata ALBUMARTIST="Gabriel Burns" -metadata TRACKNUMBER="3" -metadata DISCNUMBER="2" \
  -metadata GROUPING="Gabriel Burns" -metadata DATE="2014-08-01" -metadata SERIES="Gabriel Burns" \
  hoerspiel_tags.flac
