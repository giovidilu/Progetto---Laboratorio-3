# Relazione — Progetto Connections (Lab III, UniPi)

> **Nota di lavoro**: questo è un documento di lavoro (bozza), da aggiornare man mano che si completano le parti mancanti (UDP, packaging). La versione finale da consegnare deve essere un **PDF di massimo 5 pagine** e deve contenere esattamente le 4 sezioni + il manuale d'uso richiesti dalla specifica (sezione 4). Questo file segue già quella struttura.

---

## 1. Scelte di interpretazione personale

*(Punti del progetto lasciati alla libera interpretazione, con relativa motivazione)*

- **Separazione ortogonale `MoveOutcome` / `GameOutcome`**: l'esito della singola proposta (`CORRECT`/`WRONG`/`MALFORMED`/`ALREADY_COMPLETED`) e l'esito finale della partita per il giocatore (`WON`/`LOST_BY_MISTAKES`/`DID_NOT_FINISH`) sono modellati come due campi distinti in `ProposalResult`, non un unico enum con tutte le combinazioni esplicite. Motivazione: una singola mossa può essere sia `CORRECT` sia contestualmente concludere la partita (es. terzo gruppo trovato) — un enum piatto non potrebbe rappresentare entrambe le informazioni contemporaneamente senza perderne una.
- **CQS (Command-Query Separation)** applicato rigorosamente in `getGameInfoForPlayer`/`getGameStats`: le query di sola lettura non usano `computeIfAbsent` e non creano mai una entry in `activePlayerStates` come effetto collaterale. L'inizializzazione dello stato di un giocatore avviene **solo** alla prima `submitProposal` (unico punto che modifica legittimamente lo stato).
- **`DID_NOT_FINISH` come convenzione implicita, non campo persistito**: `PlayerGameState.getOutcome()` è un metodo derivato da `correctGroups`/`mistakes`, non un campo memorizzato. Un `outcome == null` letto da un `PlayerGameState` archiviato dentro un `GameRecord` concluso è interpretato per convenzione come DNF, evitando di duplicare l'informazione (single source of truth).
- **Aggiornamento tempestivo delle statistiche utente**: `UserStats.recordGameResult(...)` viene invocato immediatamente in `submitProposal` non appena l'esito di un giocatore diventa definitivo (`WON`/`LOST_BY_MISTAKES`), non solo alla rotazione globale del round. `rotateGame()` si occupa esclusivamente dei giocatori rimasti con `outcome == null` allo scadere del tempo (convertiti in `DID_NOT_FINISH`). Questo evita che un giocatore debba attendere la fine dell'intero round per vedere aggiornate le proprie statistiche personali e la classifica, coerentemente con [sezione 2.2 della specifica].
- **`MoveOutcome.MALFORMED` → `ResponseCode.MALFORMED_PROPOSAL`** (errore di protocollo), non una risposta `SUCCESS` con payload: scelta guidata dal testo esplicito della specifica ("notificate come errori al giocatore").
- **`ALREADY_COMPLETED` → `ResponseCode.BAD_REQUEST`**: un tentativo di proposta su una partita già conclusa per il giocatore (o a tempo scaduto) è trattato come richiesta non valida rispetto allo stato corrente, senza mutare lo stato né consumare tentativi.
- **Riuso del campo `entries` di `LeaderboardPayload`** sia per la classifica generale/top-K sia per la richiesta di un singolo `playerName` (lista con un solo elemento in quel caso), evitando di estendere un DTO di protocollo già definito quando non strettamente necessario.
- **Scheduling**: `scheduleWithFixedDelay` per `PersistenceManager` (evita sovrapposizioni di scritture I/O potenzialmente lente) vs. `scheduleAtFixedRate` per la rotazione partite in `GameManager` (operazione rapida in memoria; la durata del round deve restare fedele al valore configurato, senza deriva temporale cumulativa).
- *(Da completare dopo Macro-4)*: audience e contenuto esatto della notifica UDP di fine partita/round.

---

## 2. Schema dei thread

### Lato server
| Thread | Ruolo |
|---|---|
| Main (accept loop) | `ServerSocket.accept()` bloccante, delega ogni connessione al thread pool |
| Worker pool (`newCachedThreadPool`) | Un thread per connessione client attiva, esegue `ClientHandler` |
| Timer `GameManager` (`ScheduledExecutorService` mono-thread) | Esegue `rotateGame()` a cadenza fissa (`scheduleAtFixedRate`) |
| Timer `PersistenceManager` (`ScheduledExecutorService` mono-thread) | Esegue `saveAll()` periodico (`scheduleWithFixedDelay`) |
| *(da aggiungere)* Thread ricezione/invio UDP | Macro-4 |

### Lato client
| Thread | Ruolo |
|---|---|
| Main | Loop CLI, invio richieste TCP sincrono (`SocketChannel` bloccante, Phase 5.1) |
| *(da aggiungere)* Thread ricezione asincrona UDP | Phase 5.2 |

---

## 3. Strutture dati utilizzate

### Lato server
- `SessionManager`: `Set<String>` da `ConcurrentHashMap.newKeySet()` — utenti attualmente loggati.
- `UserRepository`: mappa utenti persistita in JSON (verificare tipo esatto di collezione interna).
- `GameRepository`: `ConcurrentHashMap<Integer, GameRecord>` — storico partite concluse.
- `GameManager`:
  - `activeGame` (campo semplice, protetto da monitor intrinseco `synchronized`)
  - `activePlayerStates`: `ConcurrentHashMap<String, PlayerGameState>` — stato mutabile dei giocatori nella partita attiva
  - `templates`: `Map<Integer, GameTemplate>` immutabile (`Collections.unmodifiableMap`), caricata una sola volta all'avvio
- `GameRecord`: snapshot immutabile (a livello di costruttore; nota: Gson bypassa questa garanzia in deserializzazione via reflection) con `allGroups` e `playerStates`.

### Lato client
- *(da completare: strutture usate in `CommandLineInterface`/`ServerConnection`)*

---

## 4. Primitive di sincronizzazione

- **`GameManager`**: tutti i metodi che leggono/modificano `activeGame`/`activePlayerStates`/`currentGameId` sono `synchronized` sullo stesso monitor intrinseco (`this`). La rientranza dei lock Java permette a `rotateGame()` (synchronized) di chiamare `startNewActiveGame()` (anch'esso synchronized) senza deadlock.
- **`lifecycleLock` dedicato**: `start()`/`stop()` del timer interno di `GameManager` usano un monitor **separato** da quello di dominio, per evitare che lo shutdown hook resti bloccato in attesa che una rotazione in corso (che detiene il lock di dominio) termini, mentre a sua volta l'operazione di stop dovrebbe attendere la terminazione del task schedulato — un rischio di stallo temporaneo diagnosticato e risolto disaccoppiando i due monitor.
- **`UserStats`**: tutti i metodi (getter e `recordGameResult`) sono `synchronized` sullo stesso oggetto istanza, per garantire atomicità e visibilità tra il thread di rotazione di `GameManager` (scrittore) e i thread `ClientHandler` (lettori, per `requestPlayerStats`/`requestLeaderboard`).
- **`SessionManager`**: `ConcurrentHashMap.newKeySet()` + operazione atomica `add()` (basata su `putIfAbsent` interno) per prevenire race condition di tipo check-then-act sul doppio login, senza necessità di un blocco `synchronized` a grana grossa.
- **Persistenza**: `UserRepository`/`GameRepository` con metodi `synchronized` per l'accesso alla mappa interna, condivisi tra i thread `ClientHandler` e il thread periodico di `PersistenceManager`.

---

## 5. Manuale di istruzioni (compilazione, esecuzione, comandi)

*(da completare a fine progetto — bozza dei punti da includere)*

- Requisiti: JDK, libreria Gson (`lib/gson-2.10.1.jar`).
- Comando di compilazione da riga di comando:
  ```
  javac -cp "lib/*:src" -d bin $(find src -name "*.java")
  ```
- Avvio server: `java -cp "bin:lib/*" server.ServerMain`
- Avvio client: `java -cp "bin:lib/*" client.ClientMain`
- File di configurazione: `config/server.properties`, `config/client.properties` (elencare ogni parametro e significato).
- Sintassi dei comandi lato client (menu CLI) — screenshot o elenco numerato delle opzioni.
- *(da aggiungere)*: istruzioni packaging JAR eseguibili separati per client e server.

---

## Checklist di completamento (uso interno, non va nella relazione finale)

- [x] Macro-0, Macro-1, Macro-2, Phase 5.1
- [x] Macro-3 (concorrenza base)
- [x] Macro-6 (ciclo di vita partita, `GameManager` completo)
- [x] `requestGameStats`, `requestLeaderboard`, `requestPlayerStats`
- [ ] Macro-4 (UDP server) — **in corso di progettazione**
- [ ] Phase 5.2 (UDP client)
- [ ] Sezione 2 e 3 della relazione da completare con dettagli lato client (chiedere/verificare struttura `ServerConnection`/`CommandLineInterface`)
- [ ] Manuale d'uso completo
- [ ] Packaging JAR
- [ ] Rilettura finale relazione (limite 5 pagine!)