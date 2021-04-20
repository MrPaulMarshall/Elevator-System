# Elevator System

---

Uproszczony system do zarządzania windami w budynku.

Na każdym piętrze budynku działa proces przyjmujący zgłoszenia pasażerów
i komunikujący je za pomocą połączenia TCP do serwera.
Serwer przyjmuje te zgłoszenia, wyznacza do ich realizacji
konkretne windy i wysyła odpowiedzi do procesów-klientów.

Taka interpretacja problemu to niepełne odwzorowanie rzeczywistości,
jednak uwzględnia istotę sprawy, czyli poruszające się niesynchronicznie
windy oraz pasażerów chcących dostać się za ich pomocą na inne piętra.

## Struktura projektu

Zadanie można rozłożyć na dwie części:
* komunikacja klientów z serwerem
* sterowanie ruchem wind

### Moduł komunikacji:

***FloorClient***
reprezentuje proces klienta, np. terminalu przy windach

- pozwala pasażerowi zażądać windy na określone piętro oraz przeczytać
  uzyskaną odpowiedź
  
- główny wątek, realizujący metodę `main`,
  nawiązuje połączenie z serwerem, tworzy wątek ***Listener*** po czym
  rozpoczyna czytanie żądań od pasażerów i wysyłanie ich do serwera.
  
***FloorClient#Listener*** wewnętrzna klasa — wątek procesu ***FloorClient***

- odpowiedzialny za nasłuchiwanie na wiadomości z serwera i reagowanie na nie.
  
***FloorConnectionThread*** to wątek działający w procesie serwera odpowiadający
  za komunikację z procesem ***FloorClient***.
  
***Server#RegisteringThread*** wewnętrzna klasa, pojedynczy wątek procesu serwera

-  zajmuje się nasłuchiwaniem i nawiązywaniem nowymi połączeń z
  procesami klientów.
  
***Message***
to klasa reprezentująca wiadomości przesyłane pomiędzy klientami i serwerem

- obowiązkowym polem jest w niej typ, np. **exit**, **pickup** albo **result**,
- pozostałe pola są opcjonalne i zależą od typu, np. pole **number** w wiadomości typu **ID**.
  
### Moduł sterowania windami:

***Elevator*** klasa reprezentująca windę

- przechowuje aktualne informacje o windzie
- wątek, który cyklicznie stara się realizować polecenia wydawane mu przez
  ***ElevatorManager***.
  
***ElevatorManager*** serce modelu logicznego 

- tworzy i przechowujące referencje do wind
- monitoruje ich status
- wydaje im polecenia
- jeżeli proces-klient żąda transportu na jakieś piętro,
  to właśnie ten obiekt podejmuje decyzję o tym, która winda ma to żądanie spełnić.
  
***ElevatorStatus*** to klasa reprezentująca status konkretnej windy, czyli krotkę:

- (ID, ostatnio odwiedzone piętro, następne piętro, czy jest w ruchu).
  
### Server

***Server*** klasa na styku powyższych modułów, której proces jest
odpowiedzialny za rozpoczęcie pracy systemu, wykorzystanie modułu sterowania
windami do zaspokajania potrzeb pasażerów oraz zakończenie pracy systemu.

## Uruchomienie projektu

Projekt należy zaimportować do jakiegoś środowiska, np. IntelliJ, załączyć
do niego bibliotekę Lombok, po czym zbudować go.

Następnie należy uruchomić proces serwera `Server::main`, podać ilość pięter w budynku i
określić liczbę wind. W tym momencie serwer rozpoczyna normalny cykl pracy,
tzn. nasłuchuje na połączenia klientów, a jednocześnie reaguje na komendy użytkownika
podane w konsoli:

* **status** pobiera i wypisuje na ekran położenie oraz czynności wind.

* **exit** zamyka połączenia z klientami i kończy działanie systemu.

Kiedy serwer już pracuje można przystąpić do uruchomienia klientów `FloorClient::main`.
Każdy klient, o ile nie wystąpi żaden błąd, automatycznie nawiąże kontakt z serwerem,
otrzyma unikalny numer ID (który mówi, na którym piętrze jest uruchomiony)
i rozpoczyna nasłuchiwanie na komendy użytkownika:

* **pickup floor** gdzie floor to numer piętra, na które pasażer chce się dostać.

Próba utworzenia większej liczby klientów niż zadeklarowanych przy uruchomieniu serwera
zakończy się niepowodzeniem, gdyż każdy nadmiarowy proces otrzyma negatywne ID, co sprawi,
że zakończy on swoje działanie. Klienci utworzeni, gdy serwer nie nasłuchuje na nowe
połączenia, również natychmiast się zakończą.
