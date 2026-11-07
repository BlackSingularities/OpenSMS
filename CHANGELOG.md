# Zmiany

## 1.4.2

- Zgłaszanie błędów i sugestii: nowa pozycja „Zgłoś błąd lub sugestię” – opis trafia prosto do autora przez redzikowski.com, e-mail jest opcjonalny.
- Po nieoczekiwanym zamknięciu aplikacja przy następnym uruchomieniu proponuje wysłanie raportu o błędzie – tylko za zgodą użytkownika.
- „Dodaj do kontaktów” w menu rozmowy znów działa także z aplikacjami kontaktów, które nie obsługują „dodaj lub edytuj” (np. OpenDial); gdy żadna aplikacja nie odpowie, pojawia się komunikat.
- Nazwy nadawców odświeżają się od razu po dodaniu albo zmianie kontaktu (także na karcie SIM) – bez restartu aplikacji.
- Poprawka budowania: wersje 1.4.0 i 1.4.1 nie powstały przez zasób o nazwie zastrzeżonej w Javie.

## 1.4.1

- **Wybór języka**: Ustawienia → System → Język – „Automatycznie” (jak telefon) albo dowolny dostępny język; ten sam wybór jest w systemowych ustawieniach języka aplikacji (Android 13+).
- **Łatwe dodawanie języków**: nowy plik `values-xx/strings.xml` wystarczy – lista języków w aplikacji i w systemie tworzy się sama. Instrukcja w `TRANSLATING.md`, a test pilnuje zgodności tłumaczeń.

## 1.4.0

- **Język angielski**: wszystkie napisy przeniesione do zasobów; aplikacja mówi po polsku albo po angielsku zgodnie z językiem telefonu (można go też wybrać osobno dla OpenSMS w ustawieniach systemu). Daty i dni tygodnia także w języku telefonu.
- **Filtr spamu**: podejrzane wiadomości od nieznanych nadawców (fałszywe dopłaty do paczek, „blokada konta”, skrócone linki) trafiają do zatrzymanych zamiast do skrzynki. Działa wyłącznie w telefonie; przełącznik w Ustawieniach → Prywatność i filtry.
- **Reakcje**: „Reacted 👍 to …”, „Liked …”, „Polubiono …” z Google Messages i iPhone'ów pokazują się jako emoji pod wiadomością, a nie jako osobne dymki. Przytrzymaj wiadomość, żeby samemu zareagować.
- **Zaznaczanie wielu wiadomości**: „Zaznacz” w menu wiadomości, potem kopiowanie, przekazanie dalej albo usunięcie kilku naraz.
- **Blokada aplikacji** odciskiem palca, twarzą albo PIN-em ekranu blokady; podgląd w ostatnich aplikacjach jest wtedy zasłonięty.
- **Treść na ekranie blokady** – nowy przełącznik; domyślnie ekran blokady pokazuje tylko „Nowa wiadomość”.
- **Skróty rozmów**: powiadomienia trafiają do sekcji „Rozmowy” Androida (można oznaczyć rozmowę jako priorytetową), a rozmowy pojawiają się w arkuszu udostępniania.
- **Czytnik ekranu**: dymki podają nadawcę i godzinę, akcje przesunięcia na liście są dostępne w menu „Działania” TalkBacka, opisane przyciski i stany.
- Nowe testy: reakcje, filtr spamu, kompletność tłumaczeń i odmiana liczebników.

## 1.3.2

- Bez żadnych wzmianek o GitHubie w aplikacji: nowy adres serwera aktualizacji, komunikaty błędów bez nazw zewnętrznych serwerów.

## 1.3.1

- Komunikat o nowej wersji bez listy zmian, linków i informacji, skąd pobierana jest aktualizacja.

## 1.3.0

- **Emotikony jako emoji**: `:)`, `;)`, `:D`, `XD`, `<3` i kilkadziesiąt innych wzorów pokazuje się w rozmowie i na liście jako emoji – także w środku zdania i zaraz po wyrazie („ok;)”). Sama emotikona w wiadomości rośnie jak duże emoji. Adresy `http://` i zwykłe wyrazy zostają nietknięte, a w bazie zapisana jest oryginalna treść. Przełącznik w Ustawieniach → Wygląd rozmowy.
- **Zaokrąglenie dymków suwakiem** (0–28 dp) zamiast trzech gotowych kształtów, z podglądem na żywo i przyciskiem powrotu do domyślnych 22 dp.
- Nagłówek daty nad wiadomościami bez tła i obwódki – sam napis.
- Zniknął pusty pas między paskiem stanu a treścią: wszystkie widoki zaczynają się od razu pod paskiem.

## 1.2.1

- Baner aktualizacji w aplikacji pokazuje tylko numer nowej wersji, bez listy zmian z GitHuba.

## 1.2.0

- **Przełączanie kart gestem**: przesunięcie palcem w bok po pustym miejscu listy (pod rozmowami albo na pustej liście) zmienia kartę – Wszystkie, Nieprzeczytane, Archiwum itd.
- **Nowa wyszukiwarka**: wyrasta z lupy i płynnie rozciąga się w lewo, zakrywając karty; lista rozmów znika dopiero po wpisaniu tekstu.
- **Powiększanie dwoma palcami także na liście rozmów**, zapamiętywane osobno od rozmowy.
- **Kody jednorazowe**: zamiast przycisku pod dymkiem – mały, dyskretny znaczek kopiowania w prawym górnym rogu kodu; podgląd w ustawieniach wygląda tak samo.
- Komunikat „Nie można odpowiedzieć” na całą szerokość, z tłem zanikającym ku górze i wyśrodkowanym nagłówkiem.
- Liczniki na zakładkach nie są już przycinane przez krawędź przycisku.
- **Planowanie z potwierdzeniem**: w arkuszu „Zaplanuj wysłanie” przełącznik „Zapytaj przed wysłaniem” – o wybranej godzinie przychodzi powiadomienie z przyciskami „Wyślij” i „Nie wysyłaj”; bez niego wiadomość wychodzi sama.
- Uporządkowany wygląd ustawień: równe wysokości i odstępy wierszy, wyrównane nagłówki sekcji, czytelniejsze opisy.

## 1.1.0

- **Powiększanie dwoma palcami** w rozmowie: dymki, tekst, odstępy i zdjęcia płynnie rosną i maleją; plakietka pokazuje procent, dotknięcie wraca do 100%.
- **Wygląd rozmowy** w ustawieniach z podglądem na żywo: kolor własnych wiadomości (biały, srebrny, grafit, kontur), kształt i szerokość dymków, odstępy, kiedy pokazywać godzinę, duże emoji, nagłówki dni i awatary w rozmowach grupowych.
- **Kody jednorazowe**: kod w treści jest wyróżniony i kopiuje się po dotknięciu; pod wiadomością dyskretny przycisk z kodem, który po skopiowaniu na chwilę pokazuje „Skopiowano”.
- Po wysłaniu pole pisania od razu się czyści i tekst nie zostaje już jako szkic; przy błędzie wysyłki treść wraca do pola.
- Przełączniki w ustawieniach nie wychodzą poza ekran, podpowiedź w polu pisania ma ten sam rozmiar co wpisywany tekst, dymki dopasowują szerokość do ekranu.

## 1.0.3

- Liczniki na zakładkach w kolorze akcentu, przypięte do rogu ikony – nie poszerzają już przycisków.
- Wspólna plakietka licznika dla zakładek i przycisku menu, stała wysokość zakładek i płynna zmiana koloru.
- „Szkic:” na liście rozmów w kolorze akcentu (czerwień zostaje dla błędów i usuwania).

## 1.0.2

- Dopasowane kolorystycznie tła ikon aplikacji i spójne zasoby graficzne.
- Nowa ikona aplikacji, warianty systemowe i odświeżona grafika na stronie projektu.

## 1.0.0

- Nowa strona aplikacji z opisem funkcji.
- Jednolita informacja o autorze (Adam Rędzikowski · redzikowski.com) i przycisk „Postaw kawę”.
- Usunięte odnośniki do repozytorium z interfejsu.

Pierwsza stabilna wersja OpenSMS.

- **Kosz**: usunięte rozmowy czekają 30 dni na przywrócenie; można je też usunąć na zawsze albo opróżnić kosz.
- **Cofnij**: po archiwizacji, przypięciu, oznaczeniu, wyciszeniu, blokadzie i usunięciu komunikat ma przycisk „Cofnij”.
- **Filtry jako ikony**: kategorie u góry pokazują same ikony, nazwę ma tylko otwarta.
- Wydania powstają z tagów `vX.Y.Z`.

## 0.1.0

Pierwsze wydanie OpenSMS – pełna aplikacja do SMS i MMS w stylu OpenPDF.

- **SMS i MMS**: odbieranie, wysyłanie, wiadomości wieloczęściowe, raporty doręczenia, ponawianie nieudanych.
- **MMS**: zdjęcia, filmy, nagrania, pliki i wizytówki; zdjęcia zmniejszane do limitu operatora (300 KB / 600 KB / 1 MB); automatyczne lub ręczne pobieranie; rozmowy grupowe.
- **Rozmowy**: przypinanie, archiwum, wyciszanie, oznaczanie jako (nie)przeczytane, gesty przesunięcia, zaznaczanie wielu, szkice.
- **Wyszukiwanie** w treści SMS i MMS oraz w kontaktach, z podświetleniem wyników.
- **Powiadomienia**: styl rozmowy, odpowiedź bez otwierania aplikacji, „Przeczytane”, kopiowanie kodów jednorazowych jednym dotknięciem.
- **Wiadomości zaplanowane**, szablony szybkich odpowiedzi, podpis, tryb bez polskich znaków (160 znaków w SMS).
- **Dual SIM**: wybór karty przy wysyłce i domyślna karta.
- **Filtr spamu**: blokowanie numerów, słów kluczowych i nieznanych nadawców; lista zatrzymanych wiadomości z przywracaniem.
- **Kopia zapasowa** SMS do pliku JSON i import bez duplikatów; statystyki.
- **Automatyczne aktualizacje** z GitHub Releases – pobieranie i instalacja jednym dotknięciem.
