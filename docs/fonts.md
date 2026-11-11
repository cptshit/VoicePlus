# Bundled fonts

The fonts in `core/ui/src/main/res/font` are subsets of the original releases (all SIL OFL):
Literata, IBM Plex Sans (both variable) and Barlow Condensed SemiBold/Bold. They only contain
Latin (Basic Latin, Latin-1, Latin Extended-A) plus common punctuation, the euro sign and a few
arrows. Anything else (Cyrillic, Greek, CJK, ...) falls back to the system font on Android.

The weight axis is limited to 400-700 (the app uses 400, 500 and 600), the unused width axis of
IBM Plex Sans is pinned to 100. The optical size axis of Literata is untouched.

To regenerate from the original files (needs `pip install fonttools`):

```sh
U="U+0020-007E,U+00A0-00FF,U+0100-017F,U+02C6,U+02DC,U+2010-2015,U+2018-201E,U+2020-2022,U+2026,U+2030,U+2039-203A,U+20AC,U+2122,U+2190-2193,U+2212,U+25CF"
pyftsubset Original.ttf --unicodes="$U" --layout-features='*' --glyph-names --notdef-outline --name-IDs='*' --output-file=Subset.ttf
fonttools varLib.instancer Subset.ttf wght=400:700 -o Out.ttf            # Literata
fonttools varLib.instancer Subset.ttf wght=400:700 wdth=100 -o Out.ttf   # IBM Plex Sans, then rename (see License)
```

Size: 1.7 MB before, about 0.6 MB after. The unmodified files are in the git history before the
commit that introduced this file.

## License

All three families are licensed under the SIL Open Font License 1.1. The full texts are in
`docs/licenses/` (`OFL-literata.txt`, `OFL-ibmplexsans.txt`, `OFL-barlowcondensed.txt`) and the
authors are credited in `CREDITS.md`.

The files in this repository are modified versions (subsets, see above) of the original fonts.
The OFL allows that and requires the license and copyright notices to stay with the fonts, which
they do.

IBM Plex Sans declares the Reserved Font Name "Plex". Clause 3 of the OFL says a modified
version must not use a Reserved Font Name, so the family in the Plex subset is renamed to
"Folgenreich Sans": name IDs 1, 3, 4, 6, 25 and the named-instance PostScript names. The copyright,
trademark and license entries of the font are left as they were. The app loads the font by its
resource id, so the rename has no effect on rendering.

Literata and Barlow Condensed declare no Reserved Font Name and keep their names.
