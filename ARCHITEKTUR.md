# Windi-Chronik – technische Struktur

Dieses Repository enthält mehrere fachlich getrennte Bestandteile. Sie bleiben getrennt gekennzeichnet, damit Wartung und Guardian-Prüfungen keine Funktionen miteinander verwechseln.

## 1. Windi-Inhalte
- `stories/` – redaktionelle Windi-Chronik
- `data/windis-books.json` – Buch-/Website-Feed

## 2. Zuhause am Bach Gäste-Check-in
- `app/` – eigenständige Android-App für den Gäste-Check-in
- App-Anzeigename: **ZAB Gäste-Check-in**
- personenbezogene Eingaben werden nicht als Browser-Entwurf persistiert
- Geräte-Backup ist deaktiviert
- Check-in-Dateien werden über Android FileProvider aus dem Cache geteilt

## 3. Einkauf
- `einkauf-angebote/` und `android-einkauf/` – Einkaufsdaten und Einkaufs-App

## Guardian
`.github/workflows/guardian-health.yml` prüft täglich sowie bei Änderungen:
- JSON-Integrität
- Aktualität der Daten
- abgelaufene Angebotszeiträume
- Datenschutz-Härtung des Gäste-Check-ins
- native Android-Dateifreigabe
- eingeschränkte WebView-Dateizugriffe

Fachlich getrennte Bereiche sollen bei künftigen Änderungen nicht gegenseitig umbenannt oder vermischt werden.
