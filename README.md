# OpenSMS

<img src="artwork/app-icon.png" alt="OpenSMS" width="128" />

**Otwarta aplikacja do SMS i MMS na Androida** – czarno-biały, szybki interfejs w stylu OpenPDF, bez reklam i śledzenia. Wiadomości zostają w telefonie; aplikacja łączy się z internetem wyłącznie po aktualizacje.

[![Build & Release](../../actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)

## Pobieranie

Najnowszy plik jest zawsze w **[Releases → Latest](../../releases/latest)**. Każdy wypchnięty tag `vX.Y.Z` buduje i publikuje nową wersję automatycznie, a aplikacja sama proponuje aktualizację.

1. Pobierz `OpenSMS-x.y.z.apk` na telefon.
2. Otwórz plik i zezwól na instalację z tego źródła.
3. Przy pierwszym uruchomieniu ustaw OpenSMS jako **domyślną aplikację SMS** – Android pozwala odbierać i wysyłać SMS tylko jednej aplikacji.
4. Wymagany Android 10 (API 29) lub nowszy.

## Funkcje

### Wiadomości
| Funkcja | Opis |
|---|---|
| **SMS** | Wiadomości wieloczęściowe, licznik znaków i części, raporty doręczenia, ponawianie nieudanych jednym dotknięciem. |
| **MMS** | Zdjęcia, filmy, nagrania, pliki i wizytówki (vCard). Zdjęcia zmniejszane do limitu operatora. Własny koder/dekoder PDU – wysyłanie i pobieranie bez bibliotek zewnętrznych. |
| **Rozmowy grupowe** | Jako MMS (wszyscy widzą odpowiedzi) albo osobny SMS do każdego. |
| **Linki i kody** | Klikalne adresy, e-maile i numery; przycisk „Kopiuj kod” przy kodach jednorazowych. |
| **Dual SIM** | Wybór karty przy każdej wiadomości i domyślna karta. |

### Porządek
- Przypinanie, archiwum, wyciszanie rozmów, oznaczanie jako przeczytane/nieprzeczytane.
- Gesty przesunięcia w lewo i w prawo (do wyboru: archiwizuj, przeczytane, przypnij, usuń).
- Zaznaczanie wielu rozmów, szkice zapisywane automatycznie.
- Wyszukiwanie w treści SMS i MMS (z podświetleniem), w kontaktach i numerach.

### Wygoda
- Powiadomienia w stylu rozmowy: odpowiedź bez otwierania aplikacji, „Przeczytane”, kopiowanie kodu.
- Wiadomości zaplanowane (przytrzymaj przycisk wysyłania), szablony szybkich odpowiedzi, podpis.
- Tryb „bez polskich znaków” – SMS mieści 160 zamiast 70 znaków.
- Przeglądarka zdjęć z powiększaniem, zapis załączników do galerii, przekazywanie dalej.

### Prywatność
- Blokowanie numerów, słów kluczowych i nieznanych nadawców (oraz numerów zablokowanych w systemie).
- Lista zatrzymanych wiadomości z możliwością przywrócenia.
- Ukrywanie treści w powiadomieniach.

### Kopia i statystyki
- Eksport wszystkich SMS do pliku JSON i import bez duplikatów.
- Statystyki: liczba wiadomości, najczęstsi rozmówcy, pora dnia.

## Aktualizacje

Aplikacja sprawdza przy starcie najnowsze wydanie na GitHubie. Baner „Nowa wersja” pobiera APK i instaluje go przez systemowy instalator. Od Androida 12, gdy aplikacja sama zainstalowała poprzednią wersję, kolejne aktualizacje przechodzą bez dodatkowego potwierdzenia.

## Budowanie

```bash
gradle :app:assembleDebug
```

Wymagane JDK 17 i Android SDK (compileSdk 36). Wydanie powstaje na GitHub Actions po wypchnięciu tagu `vX.Y.Z` zgodnego z plikiem `VERSION` (np. `git tag v1.0.0 && git push origin v1.0.0`); podpis z sekretów `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

### Struktura
| Katalog | Zawartość |
|---|---|
| `data/` | Dostęp do bazy SMS/MMS, kontakty, karty SIM, ustawienia, kopia zapasowa. |
| `sms/` | Wysyłanie SMS, odbiór (`SMS_DELIVER`), statusy, szybka odpowiedź z ekranu połączenia. |
| `mms/` | Koder/dekoder PDU, wysyłanie, pobieranie i zapis MMS. |
| `notify/` | Powiadomienia i ich akcje. |
| `schedule/` | Wiadomości zaplanowane. |
| `update/` | Aktualizacje z GitHub Releases. |
| `ui/` | Interfejs w Jetpack Compose. |

## Licencja

MIT – zobacz [LICENSE](LICENSE).
