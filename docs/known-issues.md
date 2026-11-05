# Bekannte Probleme (noch offen)

Notizen zu Fehlern, die beim Arbeiten aufgefallen sind und auf einem eigenen Branch behoben
werden sollen.

## MediaScanner: Bücher unter `FolderType.Author` werden mehrfach gescannt

`core/scanner/src/main/kotlin/voice/core/scanner/MediaScanner.kt`, in `scan(folders, …)`:

```kotlin
FolderType.Author -> {
  files.flatMap { folder ->
    folder.children.flatMap { author ->
      if (author.isFile) {
        listOf(author)
      } else {
        author.children.flatMap {
          author.children   // <- ignoriert das Lambda-Argument
        }
      }
    }
  }
}
```

`author.children.flatMap { author.children }` liefert die Kinder eines Autor-Ordners n-mal
(n = Anzahl der Bücher des Autors). Gemeint ist vermutlich `author.children`. Folge: Jedes Buch
wird pro Scan n-mal geparst, bei vielen Büchern pro Autor quadratisch viele Durchläufe.
Funktional fällt es kaum auf, weil die Ergebnisse über die `BookId` zusammenfallen.

Fix: `author.children` direkt verwenden, Test in `MediaScannerTest` mit einem Autor-Ordner mit
mindestens zwei Büchern, der die Anzahl der `parseAndStore`-Aufrufe prüft.
