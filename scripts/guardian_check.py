#!/usr/bin/env python3
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []
warnings = []

def fail(msg):
    errors.append(msg)
    print(f"::error::{msg}")

def warn(msg):
    warnings.append(msg)
    print(f"::warning::{msg}")

def load_json(rel):
    path = ROOT / rel
    if not path.exists():
        fail(f"Datei fehlt: {rel}")
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Ungültiges JSON in {rel}: {exc}")
        return None

def parse_dt(value):
    if not value:
        return None
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except Exception:
        return None

books = load_json("data/windis-books.json")
offers = load_json("einkauf-angebote/angebote.json")

if books:
    generated = parse_dt(books.get("generated_at"))
    if not generated:
        warn("Windis-Buchfeed hat kein auswertbares generated_at.")
    else:
        if generated.tzinfo is None:
            generated = generated.replace(tzinfo=timezone.utc)
        age = datetime.now(timezone.utc) - generated.astimezone(timezone.utc)
        if age.days > 14:
            warn(f"Windis-Buchfeed ist {age.days} Tage alt.")

if offers:
    checked = parse_dt(offers.get("checkedAt") or offers.get("updated"))
    if not checked:
        warn("Einkaufsangebote haben kein auswertbares checkedAt/updated.")
    else:
        if checked.tzinfo is None:
            checked = checked.replace(tzinfo=timezone.utc)
        age = datetime.now(timezone.utc) - checked.astimezone(timezone.utc)
        if age.days > 14:
            warn(f"Einkaufsangebote sind {age.days} Tage alt und sollten neu geprüft werden.")
    stale = 0
    today = datetime.now(timezone.utc).date()
    for item in offers.get("offers", []):
        until = item.get("validUntil")
        if until:
            try:
                if datetime.fromisoformat(until).date() < today:
                    stale += 1
            except Exception:
                pass
    if stale:
        warn(f"{stale} Einkaufsangebote haben ein bereits abgelaufenes validUntil.")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
if 'android:allowBackup="false"' not in manifest:
    fail("Check-in-App erlaubt Geräte-Backups; personenbezogene Daten könnten gesichert werden.")
if "androidx.core.content.FileProvider" not in manifest:
    fail("Sicherer FileProvider für Check-in-Dateien fehlt.")

html = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")
if "localStorage" in html:
    fail("Check-in-App speichert weiterhin personenbezogene Daten in localStorage.")
if "AndroidBridge.shareCheckin" not in html:
    fail("Native Android-Teilen-Funktion ist nicht im Check-in-Frontend verdrahtet.")

activity = (ROOT / "app/src/main/java/at/zuhauseambach/windichronik/MainActivity.java").read_text(encoding="utf-8")
for required in ["@JavascriptInterface", "FileProvider.getUriForFile", "setAllowFileAccess(false)", "setAllowContentAccess(false)"]:
    if required not in activity:
        fail(f"Android-Härtung fehlt: {required}")

print(f"Guardian-Check abgeschlossen: {len(errors)} Fehler, {len(warnings)} Warnungen.")
sys.exit(1 if errors else 0)
