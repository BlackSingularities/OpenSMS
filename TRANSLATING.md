# Tłumaczenia

OpenSMS sam wybiera język telefonu. W Androidzie 13+ można też ustawić język tylko dla aplikacji, w Ustawieniach → System → Język albo w ustawieniach systemu.

## Jak dodać nowy język

1. Skopiuj `app/src/main/res/values/strings.xml` (angielski, wzorzec) do `app/src/main/res/values-XX/strings.xml`, gdzie `XX` to kod języka: `de`, `uk`, `cs`, `pt-rBR` (wariant regionalny) albo `b+sr+Latn` (pismo).
2. Przetłumacz wartości. Nazw kluczy nie zmieniaj, a argumenty `%1$s`, `%2$s` zostaw w tekście (kolejność w zdaniu może być inna). Wpis `app_name` usuń, bo nie jest tłumaczony.
3. To wszystko. Nowy język sam trafi na listę w aplikacji (`BuildConfig.LOCALES`) i do systemowego wyboru języka (`generateLocaleConfig`).

Brakujące napisy pokazują się po angielsku, więc tłumaczenie może być częściowe. Test `StringResourcesTest` (uruchamiany w CI przed każdym wydaniem) sprawdza, że tłumaczenie nie ma kluczy spoza wzorca i że argumenty się zgadzają.

## Nowe napisy w kodzie

W kodzie nie wpisuje się tekstu na sztywno. Dodaj klucz do `values/strings.xml` i do `values-pl/strings.xml`, a w kodzie użyj `tr(R.string.klucz)` albo `tr(R.string.klucz, argument)`. Funkcja działa wszędzie: w Compose, w powiadomieniach i w odbiornikach.
