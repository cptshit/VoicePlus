# Hörspiel-Tagging: Muster in echten Sammlungen

Grundlage für den `HoerspielTagNormalizer` (Roadmap Schritt 2/3). Alle Zahlen stammen aus
`fixtures/tags/gabriel-burns.jsonl` (erzeugt mit `tools/dump-tags.sh`). Weitere Serien kommen
als eigene Abschnitte dazu.

## Gabriel Burns – Überblick

| | |
|---|---|
| Dateien | 480, alle `.mp3` |
| Folgen (Ordner) | 45 (Folge 1–45, lückenlos) |
| Struktur | `~/Hörspiele/<Folgenordner>/<Track>.mp3`, ein Ordner pro Folge, kein Serienordner darüber |
| Unterordner | nur Folge 41 (`CD1` … `CD4`) |
| Tracks pro Folge | 8–29 (meist 10) |
| Laufzeit pro Folge | 40–75 min, Ausreißer Folge 41 mit 304 min (4 CDs) |
| Folgen mit Tags | 16 (Folge 1, 31–45), jeweils alle Dateien der Folge |
| Folgen ganz ohne Tags | **29** (Folge 2–30): `tags: {}` in jeder Datei |
| Eingebettete Kapitel | keine (0 Dateien) |

Folgerung: **Der Ordnername ist die einzige Quelle, die für alle 45 Folgen Nummer und Titel
liefert.** Tags ergänzen höchstens, und sie sind uneinheitlich.

## Ordnernamen

| Muster | Folgen | Beispiel |
|---|---:|---|
| `GabrielBurns - NNN - Titel` (Serie ohne Leerzeichen, dreistellig) | 44 (2–45) | `GabrielBurns - 002 - Die Brut` |
| `Gabriel.Burns.NN.-.Titel` (Punkte statt Leerzeichen, zweistellig, `ue` statt `ü`) | 1 (1) | `Gabriel.Burns.01.-.Der.Fluesterer` |

Besonderheiten im Titelteil:

- Umlaute teils ersetzt: `Fluesterer` (1), `Weiss` (30), `weiss` (34). Andere Ordner haben echte
  Umlaute (`Die Fänge des Windes`, `Welt der Dämmerung`, `Infektiös Teil 2`).
- Zweiteiler: `Teil 1`/`Teil 2` steht im Ordner nur bei 15/16. In den Dateinamen steht es auch bei
  10–13 (`[Teil 1]`/`[Teil 2]`). **`Teil N` ist hier Teil des Folgentitels, keine CD-/Folgennummer.**
- Komma im Titel: `Die, die nicht bluten` (32). Felder nur an ` - ` trennen, nie an `,`.
- Sehr kurze Titel: `R` (26).
- Ordner und Dateien sind sich uneinig:

| Folge | Ordner | Dateiname | Album-Tag |
|---:|---|---|---|
| 11 | `Welt der Dämmerung` | `Welt der Dämmerung - Directors Cut [Teil 2]` | – |
| 13 | `Die Komission` | `Die Kommission [Teil 2]` | – |
| 24 | `Der erste der zehn` | `Der Erste der Zehn` | – |
| 25 | `Dem Winter folgt der Herbst` | `Dem Winter folgte der Herbst` | – |
| 34 | `Ich weiss was Angst ist` | – | `F34-Ich weiss, was Angst ist` |

Welche Schreibweise jeweils stimmt, kann die App nicht wissen. Das gehört in den Sammel-Editor
(Roadmap 5).

Entschieden (Abgleich mit MusicBrainz, gesetzt von Hand über „Buch bearbeiten“; der Normalizer
bleibt beim Ordnernamen und meldet die Alternative als Abweichung):

| Folge | Titel |
|---:|---|
| 11 | `Welt der Dämmerung` (Ordner) |
| 13 | `Die Kommission` |
| 15 | `Ohne Bewusstsein` |
| 16 | `Infektiös` |
| 24 | `Der erste der zehn` (Ordner) |
| 25 | `Dem Winter folgte der Herbst` |
| 34 | `Ich weiss, was Angst ist` |

„Teil N“ gehört nie zum Titel: Der Normalizer schneidet `Teil 1`, `[Teil 2]`, `(Teil 2)` am
Titelende ab und speichert die Nummer getrennt (`episode_parts` in `hoerspiel.db`). Nach dem Scan
bekommen aufeinanderfolgende Folgen einer Serie, deren Teile ab 1 hochzählen, die Teil-Anzahl
(10/11, 12/13, 15/16 → je 2). Die Karte zeigt dann „Zweiteiler · Teil 1 von 2“ unter dem Titel. Festgehalten in
`HoerspielTagNormalizerTest.localGabrielBurnsTitleDecisions` (läuft nur mit dem lokalen Dump).

## Dateinamen

Pro Folge ist das Muster einheitlich. Platzhalter: `NN` = Tracknummer, `T` = Folgentitel.

| Muster | Folgen | Dateien | Folgennummern |
|---|---:|---:|---|
| `NN - T` | 16 | 156 | 2, 3, 9, 11–15, 17, 18, 21–26 |
| `NN-T` | 9 | 95 | 4–8, 10, 16, 19, 20 |
| `NN - Kapitel NN` | 3 | 28 | 1, 35, 44 |
| `NN-AudioTrack NN` | 3 | 30 | 31–33 |
| `Track NN` | 3 | 30 | 38, 39, 42 |
| `NNN - T - NN` (Folgennummer vorn, Track hinten) | 2 | 20 | 27, 28 |
| `NN T` (ohne Bindestrich) | 2 | 20 | 36, 37 |
| `Gabriel Burns - NN - Track NN` | 2 | 39 | 40, 45 |
| `DD-NN T` (CD-Track, `01-01 Zwei Horizonte`) | 1 | 10 | 29 |
| `NN-Gabriel Burns - NN - T` | 1 | 10 | 30 |
| `NN-gabriel_burns_-_angst-kapitel_NN-fkkab` (Ripper-Kürzel) | 1 | 10 | 34 |
| `NN-gabriel_burns-track_NN` | 1 | 10 | 43 |
| `CDn/NN - Kapitelname` (echte Kapitelnamen) | 1 | 22 | 41 |

- Nur Folge 41 hat **aussagekräftige** Tracknamen (`Einleitung`, `Prolog`, `Kapitel 04a`,
  `Kapitel 04b`, …, `Epilog`). Dort laufen Kapitel über CD-Grenzen (`04a` auf CD1, `04b` auf CD2).
- Alle anderen Tracknamen sind generisch (`Track 01`, `AudioTrack 01`, `Kapitel 01`) oder
  wiederholen nur den Folgentitel.
- Der erste Track ist bei 34 von 45 Folgen kürzer als 75 s (meist etwa 68 s: Intro/Titelmusik).
  Taugt höchstens als Hinweis, nicht als Regel.

## Tags (nur die 16 getaggten Folgen)

| Feld (ffprobe) | Dateien | Werte |
|---|---:|---|
| `artist` | 189 | immer `Gabriel Burns`, also Serie statt Autor |
| `album` | 189 | Folgennummer + Titel in 7 Varianten (siehe unten) |
| `title` | 189 | siehe unten |
| `track` | 189 | `1`, `01`, `1/10`, `01/10`, bei 41 pro CD neu (`01/06` … `01/04` …) |
| `date` | 179 | Jahr (`2003` … `2014`), einmal `2014-08-01` |
| `genre` | 139 | `Hörspiel`, `Hoerspiel`, `Hoerbuch`, `Speech`, `Vocal`, `Horror`, `Unknown`, `Hörspiel * Erwachsene` |
| `disc` | 32 | `1/1` (37), `1/4` … `4/4` (41) |
| `album_artist` | 10 | `Gabriel Burns` (32) |
| `publisher` | 32 | `Decision Products` (41, 43) |
| sonstiges | – | `TLEN`, `encoder`, `encoded_by`, `comment`, `language`, TXXX `DESCRIPTION`/`SOURCE`/`RIPPER` (41), cdtag-Felder (43) |

Nicht vorhanden: `MVNM`/`MVIN`, `grouping`, `composer`, eingebettete Kapitel. Die heute
ausgewerteten Serien-Tags helfen bei dieser Sammlung also gar nicht.

**Album-Tag (pro Folge einheitlich):**

| Muster | Folgen | Beispiel |
|---|---:|---|
| `(NN) T` | 1, 35, 41 | `(01) Der Fluesterer` |
| `NN/T` | 37, 40, 45 | `40/Livrare Opt` |
| nur `T` | 31, 38, 39, 44 | `Rand der Gezeiten` |
| `NN - T` (auch mit Doppel-Leerzeichen) | 32, 42 | `32 -  Die, die nicht bluten` |
| `FNN-T` / `FNN_T` | 34, 43 | `F43_Fern von allen Tiefen` |
| `NN T` | 33 | `33 Schmerz` |
| `Gabriel Burns NN - T` | 36 | `Gabriel Burns 36 - Am Ende aller Tage` |

**Title-Tag:**

| Muster | Folgen |
|---|---|
| `Kapitel NN` | 1, 35, 44 (und Teile von 41) |
| `Track NN` | 38, 39, 42, 43 |
| `AudioTrack NN` | 31–33 |
| `Angst-Kapitel NN` | 34 |
| Folgentitel in jedem Track | 36, 37 |
| `Gabriel Burns - NN` in jedem Track | 40, 45 |
| echte Kapitelnamen | 41 |

## Was die App heute daraus macht

Hergeleitet aus `BookParser.parse` und `ChapterParser.chapterName`:

| Fall | Buchname | Autor | Kapitelnamen |
|---|---|---|---|
| ungetaggt (2–30) | Ordnername `GabrielBurns - 002 - Die Brut` | – | Dateiname `01 - Die Brut`, `02 - Die Brut`, … |
| 1 | `(01) Der Fluesterer` | Gabriel Burns | `Kapitel 01` … |
| 31–33 | Album-Tag | Gabriel Burns | `AudioTrack 01` … |
| 36, 37 | `Gabriel Burns 36 - Am Ende aller Tage`, `37/Der Atem der Fahlen` | Gabriel Burns | **alle gleich**: Folgentitel |
| 40, 45 | `40/Livrare Opt` | Gabriel Burns | **alle gleich**: `Gabriel Burns - 40` |
| 41 | `(41) Verehrung` | Gabriel Burns | `Einleitung`, `Prolog`, … (gut) |

Die Bibliothek mischt also drei Namensformen. `series`/`part` bleiben überall leer, und
alphabetisch sortiert landen `(01) …`, `37/…`, `F43_…` und `GabrielBurns - …` durcheinander.

Mehr-CD: Folge 41 wird schon heute zu **einem** Buch. `ChapterParser` sammelt rekursiv alle
Dateien unter dem Folgenordner, und die natürliche URI-Sortierung ergibt `CD1/01` … `CD4/07`.
Folge 29 (`01-01` …) ist nur eine CD.

## Cover

`CoverScanner` sucht in dieser Reihenfolge:

1. Bilddateien (`image/*`) direkt im Buchordner. `cover.*`, `folder.*` und `front.*` gehen vor,
   sonst entscheidet der Dateiname (vorher: Reihenfolge von `listFiles()`). Unabhängig von
   „Datei-Tags ignorieren“.
2. Eingebettetes Cover der ersten 5 Tracks (nicht bei „Datei-Tags ignorieren“).
3. **Nur für Bücher mit Serie** (Feature-Flag `hoerspiel_normalizer`): Bilddateien im Ordner
   darüber, solange der noch im freigegebenen Ordner liegt. Ohne die Serien-Bedingung würde ein
   beliebiges Bild im Bibliotheksordner zum Cover jedes Buchs ohne Cover.

Folge 1 (`Gabriel.Burns.01.-.Der.Fluesterer`, am 5. 11. neu kopiert) hat **kein** Bild im Ordner
und kein eingebettetes Cover. Ein `Cover.jpg` (708 × 706) liegt stattdessen direkt in
`~/Hörspiele`. Es gleicht keinem der 44 anderen Folgen-Cover und hat deren Datum (13. 10.), ist
also sehr wahrscheinlich das Cover von Folge 1. Über Schritt 3 wird es jetzt gefunden. Sauberer
wäre, es in den Folgenordner zu verschieben.

Weiterhin gilt:

- Gesucht wird nur auf oberster Ebene des Buchordners (`CD1/` nicht).
- Hat ein Buch schon ein Cover, wird nicht neu gesucht.

Der vorliegende Dump listet keine Bilder (damals listete `dump-tags.sh` nur Audiodateien).

## Mapping ffprobe → ID3 → Media3

`dump-tags.sh` zeigt ffprobe-Namen. Die App liest über Media3 die rohen ID3-Frames. Tests, die aus
dem Dump `Metadata` bauen, müssen diese Tabelle verwenden.

| ffprobe-Key | ID3v2-Frame | Media3-Eintrag | `Metadata`-Feld |
|---|---|---|---|
| `title` | `TIT2` | `TextInformationFrame(id="TIT2")` | `title` |
| `artist` | `TPE1` | `TextInformationFrame(id="TPE1")` | `artist` (Autor) |
| `album` | `TALB` | `TextInformationFrame(id="TALB")` | `album` (Buchname) |
| `album_artist` | `TPE2` | `TextInformationFrame(id="TPE2")` | `albumArtist` |
| `genre` | `TCON` | `TextInformationFrame(id="TCON")` | `genre` |
| `composer` | `TCOM` | `TextInformationFrame(id="TCOM")` | `narrator` |
| `track` | `TRCK` | `TextInformationFrame(id="TRCK")`, Wert `01/10` | `trackNumber` |
| `disc` | `TPOS` | `TextInformationFrame(id="TPOS")`, Wert `1/4` | `discNumber`, `discCount` |
| `date` | `TYER` (v2.3) / `TDRC` (v2.4) | `TextInformationFrame(id="TYER"/"TDRC")` | `year` |
| `publisher` | `TPUB` | `TextInformationFrame(id="TPUB")` | ignoriert |
| `encoder` | `TSSE` | `TextInformationFrame(id="TSSE")` | ignoriert |
| `encoded_by` | `TENC` | `TextInformationFrame(id="TENC")` | ignoriert |
| `language` | `TLAN` | `TextInformationFrame(id="TLAN")` | ignoriert |
| `TLEN` | `TLEN` | `TextInformationFrame(id="TLEN")` | ignoriert |
| `comment` | `COMM` | `CommentFrame` | ignoriert |
| `grouping` | `TIT1` | `TextInformationFrame(id="TIT1")` | `grouping`, wenn kein `GRP1` |
| – (ffprobe zeigt es nicht) | `GRP1` | **`BinaryFrame`** | `grouping` |
| – (ffprobe zeigt es nicht) | `MVNM` / `MVIN` | **`BinaryFrame`** | `series` / `part` |
| `<Beschreibung>`, z. B. `MVNM`, `DESCRIPTION` | `TXXX` | `TextInformationFrame(id="TXXX", description="<Beschreibung>")` | `MVNM`, `MVIN`, `PART`, `NARRATOR` |
| `id3v2_priv.<owner>` | `PRIV` | `PrivFrame` | ignoriert |

MP4/M4B: Media3 übersetzt iTunes-Atome in dieselben IDs (`©grp`→`TIT1`, `trkn`→`TRCK`,
`disk`→`TPOS`, `aART`→`TPE2`, `©day`→`TDRC`). `©mvn`/`©mvi` kommen als
`TextInformationFrame(id="MVNM"/"MVIN")`, also nicht als TXXX.

Vorbis (FLAC/OGG/Opus): `ALBUMARTIST`, `TRACKNUMBER`, `DISCNUMBER`, `DISCTOTAL`, `GROUPING`,
`DATE`, `SERIES`.

Lücke im Dump: ffprobe sagt nicht, ob ein Tag ID3v1 oder ID3v2 ist, und führt beide zusammen. Media3
liest bei MP3 nur ID3v2. Sollte eine Folge nur ID3v1-Tags haben, sieht die App dort nichts. Bei
den 80 Dateien mit `TLEN` ist es sicher ID3v2, bei den übrigen 109 getaggten Dateien ist es offen.

Seit Schritt 0 schreibt `dump-tags.sh` pro MP3 `id3v2` (Version oder `null`) und `id3v1`
(`true`/`false`) sowie eigene Zeilen `"kind":"image"` für Bilddateien. Ein neuer Dump klärt
damit sowohl die ID3v1-Frage als auch, wo die Cover liegen.

## Folgennummer korrigieren

„Serie bearbeiten → Titel korrigieren → Folgen in dieser Reihenfolge nummerieren“ setzt die
Nummern ab einem Startwert. Weil der Scanner die Nummer bei jedem Scan neu aus Tags und Dateinamen
liest, steht die korrigierte Nummer zusätzlich in `episode_number_overrides` (`hoerspiel.db`);
`BookParser` bevorzugt sie. Grenzen: Die Überschreibung ist nicht im Backup-Snapshot (nach einem
Restore gilt der gesicherte Wert bis zum nächsten Rescan mit „Datei-Tags neu lesen“), und es gibt
noch keine Oberfläche, um sie wieder zu löschen.
