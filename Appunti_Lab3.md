# Appunti Lab 3

---

# Lezione 1 - Contesto e motivazione del corso

La Lezione 1 introduce i fondamenti del multithreading in Java, propedeutici alla realizzazione del server multithreaded richiesto dalle specifiche di **Connections**. Il docente sottolinea che, sebbene la programmazione concorrente sia già stata affrontata in altri corsi, l'approccio adottato in Laboratorio III privilegia **costrutti ad alto livello** (thread pooling, monitors, concurrent collections) rispetto alla gestione a basso livello di lock espliciti, con l'obiettivo di ottenere codice più leggibile, manutenibile e robusto — un principio che dovrai applicare direttamente nell'implementazione del server.

## 2. Processi e Thread: richiami concettuali

- **Processo**: programma in esecuzione, con proprio spazio di indirizzamento.
- **Thread** (light weight process): flusso di esecuzione interno a un processo, che condivide lo spazio degli indirizzi con gli altri thread dello stesso processo.
- Il multitasking a livello di thread è (in parte) controllato dal programmatore, a differenza di quello a livello di processo, gestito dal SO.
- Vantaggi del multithreading: migliore utilizzo delle risorse, throughput più elevato per applicazioni I/O-bound o computazionalmente intensive.
- Svantaggi: complessità di debugging, race conditions, deadlock, livelock, starvation — problematiche rilevanti per il server Connections, che dovrà gestire più client concorrenti in sicurezza.

## 3. Creazione e attivazione di thread in Java

Sono presentate le due modalità classiche:

1. **`implements Runnable`**: si definisce un *task* (un frammento di codice eseguibile) implementando `run()`, poi lo si passa a un oggetto `Thread`.
2. **`extends Thread`**: si effettua l'overriding diretto di `run()`.

Punto teorico rilevante: la classe `Thread` implementa a sua volta `Runnable` ed estende `Object`; se il metodo `run()` non è ridefinito per overriding, viene eseguito quello di default, che invoca l'oggetto `Runnable` eventualmente passato al costruttore.

**Perché preferire `Runnable`**: Java non supporta l'ereditarietà multipla — se la classe del task deve già estendere un'altra classe (es. un gestore di eventi), non può anche estendere `Thread`. Per il progetto Connections, questo è un punto cruciale: le classi che rappresentano le connessioni client o i gestori di richieste dovranno quasi certamente implementare `Runnable` (o meglio, come vedrai più avanti, `Callable`) per essere sottomesse a un `ExecutorService`, mantenendo la libertà di estendere altre gerarchie.

## 4. `start()` vs `run()`

Distinzione fondamentale, spesso fonte di errori: **solo `start()` crea effettivamente un nuovo thread del sistema operativo**. Se si invoca direttamente `run()`, il codice viene eseguito in modo sequenziale nel thread chiamante, senza alcuna concorrenza reale. Questo è un errore classico da evitare nell'implementazione del server: ogni gestione di connessione client deve essere avviata con `start()` (o, con i thread pool, tramite `execute()`/`submit()`).

## 5. Thread Overhead e limiti del modello

- La creazione/distruzione di thread comporta overhead di interazione JVM-SO, mai trascurabile.
- Ogni thread alloca il proprio stack: la creazione di un numero eccessivo di thread può esaurire le risorse del sistema operativo ("JAVA break").

**Collegamento diretto ai requisiti del progetto**: questo è esattamente il motivo per cui le specifiche di Connections richiedono un **server multithreaded con thread pooling** anziché la creazione di un thread per ogni client — approccio che, come mostrato in slide 38, "può diventare non sostenibile" con client numerosi o frequenti.

## 6. Thread Pooling: concetti di base

Il meccanismo del thread pool risolve il problema della gestione di un gran numero di task:
- una **coda FIFO** di task in attesa di esecuzione;
- un **insieme di thread riutilizzabili**;
- quando un thread termina un task, ritorna disponibile nel pool per il successivo.

Obiettivi: riuso dei thread, riduzione del costo di attivazione/terminazione, controllo del numero massimo di thread concorrenti — tutti requisiti impliciti nelle specifiche del server Connections.

## 7. `java.util.concurrent`: framework Executor

Introdotto in Java 5, fornisce astrazioni standardizzate:
- `Executor` / `ExecutorService`: interfacce generiche per l'esecuzione di task.
- `Executors`: classe *Factory* per creare `ExecutorService` con comportamenti predefiniti.
- I task devono essere oggetti `Runnable` (o, come vedrai in lezioni successive, `Callable` per task con valore di ritorno), sottomessi tramite `execute()` (o `submit()`).

### FixedThreadPool
- Numero **fisso** di thread, definito alla creazione.
- Se tutti i thread sono occupati, i nuovi task vengono accodati in una `LinkedBlockingQueue` **illimitata**.
- Comportamento dimostrato sperimentalmente: gli stessi thread vengono riutilizzati per più task (nome `pool-1-thread-N` ricorrente nell'output).

### CachedThreadPool
- **Nessun limite** predefinito al numero di thread: se tutti sono occupati, ne viene creato uno nuovo.
- Un thread inattivo per 60 secondi viene terminato ("elasticità": il pool si espande e si contrae dinamicamente).
- Utilizza una `SynchronousQueue` (capacità 1).

**Rilevanza per Connections**: la scelta tra `FixedThreadPool` e `CachedThreadPool` (o una configurazione personalizzata tramite `ThreadPoolExecutor`) è una decisione di design che dovrai giustificare nella relazione, in base al numero atteso di client concorrenti e al carico previsto sul server.

---

# Codice Utile dal File

## 1. Task tramite `Runnable` (interfaccia)

```java
public class MyRunnable implements Runnable {
    public void run() {
        System.out.println("MyRunnable running");
        System.out.println("MyRunnable finished");
    }
}

public static void main(String[] args) {
    Thread thread = new Thread(new MyRunnable());
    thread.start();
}
```

**Rilevanza**: schema base per definire un task da eseguire in un thread separato. Nel server Connections, ogni classe che gestisce la comunicazione con un singolo client (accettazione della connessione TCP, lettura/scrittura sul socket) sarà molto probabilmente strutturata così.

## 2. Task tramite classe anonima

```java
Runnable runnable = new Runnable() {
    public void run() {
        System.out.println("Runnable running");
    }
};
Thread thread = new Thread(runnable);
thread.start();
```

**Rilevanza**: utile per task semplici e non riutilizzabili altrove — ad esempio per prototipare rapidamente la logica di gestione di una notifica UDP, prima di strutturarla in una classe dedicata.

## 3. Task con `currentThread()` per identificare il thread esecutore

```java
public class Calculator implements Runnable {
    private int number;
    public Calculator(int number) { this.number = number; }
    public void run() {
        for (int i = 1; i <= 10; i++) {
            System.out.printf("%s: %d * %d = %d\n",
                Thread.currentThread().getName(), number, i, i*number);
        }
    }
}
```

**Rilevanza**: `Thread.currentThread().getName()` è utile per il **logging e debugging** del server multithreaded — sapere quale thread del pool sta gestendo quale client è essenziale quando si debuggano race condition o si verificano i log dell'attività concorrente richiesti implicitamente da un sistema client-server robusto.

## 4. Creazione ed uso di un `FixedThreadPool`

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ExampleFixed {
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            service.execute(new Task(i));
        }
    }
}
```

**Rilevanza — CENTRALE per il progetto**: questo è lo schema diretto per implementare il server multithreaded di Connections. Il `service.execute()` sostituisce la creazione manuale di `Thread` per ogni client accettato dal `ServerSocket`.

**Nota di integrazione necessaria**: nel codice mostrato manca la chiamata a `service.shutdown()` (vista solo implicitamente); nel progetto reale dovrai gestire esplicitamente la terminazione ordinata del pool, specialmente in fase di arresto controllato del server (vedi sezione successiva).

## 5. `CachedThreadPool`

```java
ExecutorService service = Executors.newCachedThreadPool();
```

**Rilevanza**: alternativa da valutare/discutere nella relazione — utile se il numero di client è molto variabile nel tempo, ma rischiosa per assenza di limite superiore (possibile esaurimento risorse in caso di picchi di connessioni malevole o non previste).

---

# Integrazione di Codice Esterno

## 1. `ExecutorService.shutdown()` / `shutdownNow()` / `awaitTermination()`
**Package**: `java.util.concurrent.ExecutorService`

**Perché è necessario**: il file mostra come creare ed usare un thread pool, ma **non tratta la sua terminazione corretta** — aspetto obbligatorio per un server che deve poter essere arrestato in modo pulito (requisito tipico di un'applicazione di rete robusta come Connections).

```java
ExecutorService service = Executors.newFixedThreadPool(10);
// ... sottomissione dei task di gestione client ...

service.shutdown(); // non accetta nuovi task, completa quelli in corso
try {
    if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
        service.shutdownNow(); // forza la terminazione
    }
} catch (InterruptedException e) {
    service.shutdownNow();
}
```

## 2. `ThreadPoolExecutor` (costruttore esplicito)
**Package**: `java.util.concurrent.ThreadPoolExecutor`

**Perché è necessario**: `Executors.newFixedThreadPool()` e `newCachedThreadPool()` sono *factory methods* con configurazioni predefinite. Per un server di produzione, spesso è preferibile configurare esplicitamente core pool size, maximum pool size, keep-alive time e tipo di coda — parametri che potrai giustificare nella relazione del progetto.

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    20,                         // maximumPoolSize
    60L, TimeUnit.SECONDS,      // keepAliveTime
    new LinkedBlockingQueue<Runnable>(100) // coda con capacità limitata
);
```

## 3. `Callable<T>` e `Future<T>`
**Package**: `java.util.concurrent.Callable`, `java.util.concurrent.Future`

**Perché è necessario**: `Runnable.run()` non restituisce alcun valore e non può lanciare eccezioni controllate. Nel contesto di Connections, molte operazioni gestite dal server (es. validazione di una mossa, accesso a dati persistenti JSON) potrebbero richiedere un risultato o dover propagare un'eccezione — casi in cui `Callable` è preferibile a `Runnable`.

```java
ExecutorService service = Executors.newFixedThreadPool(10);
Callable<Boolean> task = () -> { return validaConnessioneClient(); };
Future<Boolean> future = service.submit(task);
boolean esito = future.get(); // bloccante finché il task non termina
```

## 4. `ConcurrentHashMap`
**Package**: `java.util.concurrent.ConcurrentHashMap`

**Perché è necessario**: il server dovrà mantenere strutture dati condivise tra i thread che gestiscono i diversi client (es. mappa client-connessi, stato delle partite attive). L'uso di una `HashMap` non sincronizzata produrrebbe race condition; `ConcurrentHashMap` offre accesso concorrente thread-safe senza il bottleneck di una sincronizzazione globale esplicita — coerente con l'approccio "ad alto livello" raccomandato nel corso.

```java
ConcurrentHashMap<String, ClientHandler> clientAttivi = new ConcurrentHashMap<>();
clientAttivi.put(idClient, handler);
```

## 5. `try-with-resources` per la gestione dei socket
**Package**: `java.lang.AutoCloseable` (usato con `java.net.Socket`, `java.net.ServerSocket`)

**Perché è necessario**: sebbene non ancora trattato nella lezione sui thread, è uno strumento essenziale della Standard Library per la gestione sicura delle risorse di rete (socket TCP) che ogni thread del pool dovrà gestire, evitando resource leak in caso di eccezione.

```java
try (Socket clientSocket = serverSocket.accept()) {
    // gestione della comunicazione col client
} catch (IOException e) {
    e.printStackTrace();
}
```
---

# Lezione 2 — ThreadPoolExecutor, ScheduledPool, BlockingQueues

---

### 1. Teoria

### 1.1 Il costruttore generale di `ThreadPoolExecutor`

Il file introduce il costruttore più generale della classe `ThreadPoolExecutor`, di cui `newFixedThreadPool` e `newCachedThreadPool` sono semplicemente istanze preconfigurate:

```java
public ThreadPoolExecutor(
    int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    RejectedExecutionHandler handler)
```

I parametri fondamentali sono:

- **`corePoolSize`**: numero minimo di thread mantenuti attivi nel pool.
- **`maximumPoolSize`**: numero massimo di thread creabili.
- **`keepAliveTime` + `unit`**: tempo oltre il quale un thread non-core, inattivo, viene terminato.
- **`workQueue`**: struttura dati (una `BlockingQueue<Runnable>`) in cui i task vengono accodati in attesa di un thread libero.
- **`handler`**: politica da applicare quando un task viene rifiutato (coda piena e pool al massimo).

**Politica di gestione dei task in arrivo** (fondamentale per capire il comportamento del server):
1. Se un thread del core è inattivo → il task gli viene assegnato.
2. Se tutti i thread del core sono occupati e la coda non è piena → il task viene accodato.
3. Se la coda è piena e non si è raggiunto `maximumPoolSize` → viene creato un nuovo thread.
4. Se la coda è piena e si è raggiunto `maximumPoolSize` → il task viene **respinto** (secondo la `RejectedExecutionHandler`, es. `AbortPolicy` che solleva `RejectedExecutionException`).

### 1.2 Rilevanza per i requisiti del progetto Connections

Questo è **il cuore dell'implementazione del server**, che il testo del progetto richiede esplicitamente come "multithreaded, realizzato usando Java thread pooling". Ogni connessione client (o ogni richiesta) può essere modellata come un `Runnable`/`Callable` sottomesso al pool. La scelta dei parametri (`corePoolSize`, `maximumPoolSize`, tipo di coda) determina come il server scala rispetto al numero di giocatori connessi contemporaneamente — un aspetto che dovrà essere giustificato nella relazione finale (sezione "schema dei thread attivati lato server").

### 1.3 Terminazione dell'Executor (Lifecycle)

Il file distingue due modalità:
- **`shutdown()`** — terminazione *graduale*: non accetta nuovi task, ma completa quelli già in coda o in esecuzione.
- **`shutdownNow()`** — terminazione *immediata* (best-effort): scarta i task in coda e tenta di interrompere i thread in esecuzione (invio di un'interruzione, non garanzia di terminazione istantanea).

Metodi di supporto: `isShutdown()`, `isTerminated()`, `awaitTermination(timeout, unit)`.

**Rilevanza per il progetto**: il server Connections deve gestire una chiusura pulita (persistenza periodica dei dati e possibilità di riavvio, come richiesto nella sezione 2.2 delle specifiche). La terminazione graduale è preferibile per non perdere richieste client in corso.

### 1.4 Dimensionamento del pool: CPU-bound vs IO-bound

Punto teorico importante:
- **Task CPU-bound** (calcoli intensivi) → dimensione ottimale ≈ numero di core disponibili (`Runtime.getRuntime().availableProcessors()`).
- **Task IO-bound** (accesso a rete, file, socket) → conviene un numero di thread **maggiore** del numero di core, perché molti thread saranno bloccati in attesa di I/O.

**Rilevanza diretta per il progetto**: il server Connections gestisce prevalentemente comunicazioni di rete (socket TCP, notifiche UDP) — quindi i task del server sono **IO-bound**. Questo è un elemento che andrà giustificato esplicitamente nella relazione, motivando la scelta della dimensione del pool.

### 1.5 `ScheduledExecutorService`

Permette di schedulare task in modo differito o periodico:
- `schedule(Runnable, delay, unit)` — esecuzione singola dopo un delay.
- `scheduleWithFixedDelay(...)` — intervallo costante **tra la fine** di un'esecuzione e l'inizio della successiva.
- `scheduleAtFixedRate(...)` — tentativo di mantenere intervalli costanti **tra gli inizi** delle esecuzioni (con "recupero" dei ritardi se un task impiega più tempo del previsto).

**Rilevanza per il progetto**: è il meccanismo naturale per implementare **la temporizzazione delle partite** ("le partite sono suddivise per periodi temporali... allo scadere del tempo il server invia notifiche"). Un `ScheduledExecutorService` può gestire lo scadere del timer di ogni partita e l'avvio automatico della partita successiva.

### 1.6 Classi thread-safe e `BlockingQueue`

Il file introduce il concetto di **sezione critica** e le alternative per ottenere classi thread-safe: strutture concorrenti predefinite (`java.util.concurrent`), monitor, lock esplicite.

In particolare, `BlockingQueue` è un'interfaccia pensata per ambienti multithread: si blocca automaticamente quando è piena (in inserimento) o vuota (in estrazione), risolvendo elegantemente il problema **produttore-consumatore**.

Le due implementazioni principali confrontate:

| Caratteristica | `ArrayBlockingQueue` | `LinkedBlockingQueue` |
|---|---|---|
| Dimensione | Fissa, obbligatoria | Opzionale (default illimitata) |
| Struttura interna | Array circolare | Lista concatenata |
| Concorrenza | Una sola lock (no inserimento/estrazione paralleli) | Lock separate per put/take (maggiore throughput) |

**Rilevanza per il progetto**: le "strutture dati opportunamente sincronizzate" richieste esplicitamente dal testo (sezione 3) per memorizzare utenti e stato del gioco possono sfruttare queste collezioni concorrenti, evitando di dover scrivere sincronizzazione manuale (`synchronized`) ovunque.

⚠️ Nota teorica importante lasciata aperta dal docente: la soluzione produttore/consumatore mostrata nel file **usa attesa attiva implicita tramite `put`/`take` bloccanti**, ma il docente segnala che linguaggi di sincronizzazione più fini (wait/notify, o costrutti di alto livello) saranno visti in una lezione successiva — utile da tenere a mente se si opterà per meccanismi di sincronizzazione più espliciti.

---

### 2. Codice Utile dal File

### 2.1 Costruttore generale con coda limitata e rejection handling

```java
import java.util.concurrent.*;

public class RejectedException {
    public static void main(String[] args) {
        ExecutorService service = new ThreadPoolExecutor(10, 12, 120, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(3));

        for (int i = 0; i < 20; i++)
            try {
                service.execute(new Task(i));
            } catch (RejectedExecutionException e) {
                System.out.println("task rejected" + e.getMessage());
            }
    }
}
```

**Perché è rilevante**: mostra come configurare esplicitamente un pool di thread con coda limitata — modello direttamente riutilizzabile per il server Connections, che deve gestire un numero potenzialmente elevato di connessioni client in ingresso.

**Nota per l'integrazione**: la classe `Task` va sostituita da un `Runnable` (o meglio, un `Callable`/task dedicato) che rappresenta la gestione di una singola richiesta client sulla connessione TCP.

### 2.2 Dimensionamento del pool in base ai core disponibili

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadDimensioning {
    public static void main(String[] args) {
        int coreCount = Runtime.getRuntime().availableProcessors();
        System.out.println(coreCount);
        ExecutorService service = Executors.newFixedThreadPool(coreCount);
        for (int i = 0; i < 100; i++) {
            service.execute(new CPUIntensiveTask());
        }
    }
}
```

**Perché è rilevante**: pattern utile per giustificare nella relazione la scelta dimensionale del pool. Poiché il server Connections è prevalentemente IO-bound (comunicazione di rete), questo pattern andrà adattato usando un numero di thread superiore a `availableProcessors()`, ma il meccanismo di lettura dei core resta utile come base di calcolo.

### 2.3 `ScheduledExecutorService` per task periodici

```java
package periodic_Shot_FixedDelay;
import java.util.concurrent.*; import java.util.*; import java.time.*;

public class FixedDelay {
    private static void periodicShot_FixedDelay() {
        ScheduledExecutorService stse = Executors.newSingleThreadScheduledExecutor();
        Runnable ru = () -> {
            System.out.println(Thread.currentThread().getName() + " start: " + Instant.now());
            // ... logica del task
        };
        // inizia dopo 2 secondi, pausa tra i thread di 3 secondi
        stse.scheduleWithFixedDelay(ru, 2, 3, TimeUnit.SECONDS);
        try {
            stse.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        stse.shutdown();
    }
}
```

**Perché è rilevante**: template direttamente applicabile alla gestione del **timer di partita**. Ad esempio, il server potrebbe usare `scheduleAtFixedRate` (o un singolo `schedule` con delay pari alla durata della partita) per determinare lo scadere del tempo e scatenare l'invio delle notifiche UDP a tutti i partecipanti, come richiesto in sezione 2.2 delle specifiche.

**Nota per l'integrazione**: per Connections è più adatto uno **`schedule` single-shot** (una sola esecuzione allo scadere del timer di partita) piuttosto che `scheduleWithFixedDelay`/`scheduleAtFixedRate`, che sono pensati per task ripetuti periodicamente.

### 2.4 Pattern produttore-consumatore con `BlockingQueue`

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class ProducerConsumerExample {
    public static void main(String[] args) {
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(3);
        Producer producer = new Producer(blockingQueue);
        Consumer consumer = new Consumer(blockingQueue);
        Thread producerThread = new Thread(producer);
        Thread consumerThread = new Thread(consumer);
        producerThread.start();
        consumerThread.start();
    }
}
```

**Perché è rilevante**: pattern generale per la condivisione sicura di strutture dati tra thread, utile ogni volta che più thread del server (es. thread che gestiscono client diversi) devono accedere in modo sincronizzato a una risorsa condivisa (es. lo stato della partita corrente, la classifica).

---

### 3. Integrazione di Codice Esterno

### 3.1 `java.util.concurrent.Callable<V>` e `Future<V>`

Il file mostra solo `Runnable`, che non restituisce un valore né può sollevare eccezioni controllate. Per il server, spesso è necessario che l'esecuzione di un task restituisca un risultato (es. l'esito della valutazione di una proposta).

```java
import java.util.concurrent.*;

Callable<Boolean> validaProposta = () -> {
    // logica di validazione, può lanciare eccezioni controllate
    return true;
};

ExecutorService pool = Executors.newFixedThreadPool(4);
Future<Boolean> risultato = pool.submit(validaProposta);
boolean esito = risultato.get(); // blocca finché il task non termina
```

### 3.2 `java.util.concurrent.ConcurrentHashMap<K,V>`

Menzionata solo di sfuggita nel file come "classe concorrente predefinita", ma non approfondita con esempio. È probabilmente la struttura più utile per il server: gestione thread-safe della mappa `username → dati utente` o `gameId → stato partita`, senza necessità di sincronizzazione manuale su un intero blocco.

```java
import java.util.concurrent.ConcurrentHashMap;

ConcurrentHashMap<String, UserSession> utentiLoggati = new ConcurrentHashMap<>();
utentiLoggati.put("giovanni", new UserSession(...));
UserSession sessione = utentiLoggati.get("giovanni");
```

**Perché necessario**: le specifiche richiedono esplicitamente "strutture dati opportunamente sincronizzate per memorizzare le informazioni relative agli utenti e allo stato del gioco". `ConcurrentHashMap` è lo strumento standard per questo scopo, non trattato nel dettaglio in questo file.

### 3.3 `java.nio.channels.Selector` e canali NIO (accennati ma non trattati qui)

Il file si concentra su thread pooling, ma il progetto richiede esplicitamente **NIO lato client** per la connessione TCP persistente. Poiché questo file non copre l'argomento, sarà necessario un materiale dedicato (probabilmente in una lezione successiva) su `SocketChannel`, `Selector`, `SelectionKey`.

```java
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

SocketChannel channel = SocketChannel.open();
channel.configureBlocking(false);
channel.connect(new InetSocketAddress("localhost", 8080));

Selector selector = Selector.open();
channel.register(selector, SelectionKey.OP_CONNECT);
```

**Nota**: segnalo questo argomento perché è **necessario** per il progetto ma non compare in questo specifico file — verrà probabilmente trattato in una lezione dedicata a NIO, da analizzare quando disponibile.

### 3.4 `java.net.DatagramSocket` / `DatagramPacket` (per le notifiche UDP)

Non presente nel file, ma richiesto esplicitamente dalle specifiche per le notifiche asincrone del server (fine partita).

```java
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

DatagramSocket socket = new DatagramSocket();
byte[] buffer = "PARTITA_TERMINATA".getBytes();
DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
        InetAddress.getByName("localhost"), 9999);
socket.send(packet);
```

**Perché necessario**: è il meccanismo standard e ben noto per implementare le notifiche UDP richieste dal progetto; non compare in questo file dedicato al thread pooling.
---
# Lezione 3

## 1. Teoria

### Callable e Future: task che restituiscono risultati

A differenza di `Runnable` (metodo `run()`, nessun valore di ritorno, non può sollevare eccezioni controllate), l'interfaccia **`Callable<V>`** definisce un solo metodo `V call() throws Exception`, che **può restituire un valore** e **sollevare eccezioni**.

Per accedere al risultato in modo asincrono si usa l'interfaccia **`Future<V>`**, restituita da `executor.submit(callable)` (non `execute()`, riservato ai `Runnable`):

| Metodo | Comportamento |
|---|---|
| `get()` | blocca fino al completamento, restituisce il risultato |
| `get(timeout, TimeUnit)` | attende al massimo `timeout`, poi solleva `TimeoutException` |
| `cancel(boolean)` | tenta di cancellare il task |
| `isDone()` / `isCancelled()` | verifica lo stato |

**Rilevanza per il progetto**: il server multithreaded di Connections userà un `ExecutorService`/thread pool per gestire le connessioni client. Se un task server-side deve restituire un risultato (es. l'esito di un'operazione di gioco elaborata in un thread separato) invece di limitarsi a notificare via socket, `Callable`/`Future` è lo strumento idiomatico, in alternativa alla gestione manuale con variabili condivise.

### Race condition e sezioni critiche

Quando più thread accedono concorrentemente a una risorsa condivisa (una struttura dati, una connessione, un contatore) senza sincronizzazione, si possono generare **race condition**: il risultato dipende dall'ordine di interleaving delle istruzioni, che è non deterministico. Anche un'istruzione apparentemente "singola" come `this.value += delta` **non è atomica** (si scompone in lettura, somma, scrittura), quindi può essere interrotta a metà da un altro thread.

Una **sezione critica** è un blocco di codice che accede alla risorsa condivisa e che deve essere eseguito da un solo thread alla volta. Una classe è **thread-safe** se il suo codice può essere usato concorrentemente senza produrre inconsistenze — “nothing bad ever happens” in nessun interleaving possibile.

**Rilevanza per il progetto**: questo è **il cuore del server multithreaded** richiesto dalle specifiche. Ogni struttura dati condivisa tra i thread che gestiscono i singoli client (es. lo stato delle partite in corso, le classifiche, le code di notifica) è potenzialmente soggetta a race condition e va protetta.

### Lock intrinseche e metodi `synchronized`

In Java **ogni oggetto** possiede una lock implicita (intrinsic lock, monitor lock). Un metodo dichiarato `synchronized`:

- richiede l'acquisizione della lock associata **all'istanza** su cui è invocato prima di eseguire;
- se la lock è già detenuta da un altro thread, il thread chiamante si sospende in una coda (**Entry Set**) gestita automaticamente dalla JVM;
- rilascia la lock al termine del metodo, sia in caso di ritorno normale sia in caso di eccezione non gestita.

Punti importanti:
- la lock è per **istanza**, non per classe: oggetti diversi della stessa classe possono eseguire metodi sincronizzati in parallelo;
- `synchronized` non è ereditato tramite overriding: va ridichiarato esplicitamente nella sottoclasse se necessario;
- i costruttori non possono essere `synchronized` (errore di compilazione) — non serve, perché di default solo il thread creatore accede all'oggetto durante la costruzione;
- non ha senso dichiarare `synchronized` nelle interfacce.

### Il Monitor: wait/notify/notifyAll

Oltre alla lock intrinseca, ogni oggetto Java possiede (concettualmente) una **wait queue** (**Wait Set**), distinta dall'Entry Set, usata per i thread che hanno acquisito la lock ma devono attendere che si verifichi una condizione sullo stato della risorsa. Tre metodi definiti in `Object`:

- **`wait()`**: sospende il thread corrente, **rilascia la lock**, fino a che un altro thread invoca `notify()`/`notifyAll()` sullo stesso oggetto. Va invocato **solo** dentro un blocco/metodo `synchronized` (altrimenti `IllegalMonitorStateException`), tipicamente in un ciclo `while` che ricontrolla la condizione.
- **`notify()`**: risveglia un singolo thread in attesa (no-op se nessuno è in attesa).
- **`notifyAll()`**: risveglia tutti i thread in attesa, che poi competono per riacquisire la lock.

**Rilevanza per il progetto**: il pattern **produttore-consumatore con monitor** (vedi sotto) è direttamente applicabile a Connections per la gestione delle **notifiche asincrone via UDP** e per qualsiasi coda condivisa tra il thread che riceve dati dal client e il thread che li elabora nel server multithreaded.

---

## 2. Codice Utile dal File

### Task che restituisce un risultato con `Callable`

```java
import java.util.concurrent.Callable;
public class Calculator implements Callable<Integer> {
    private int a;
    private int b;
    public Calculator(int a, int b) {
        this.a = a;
        this.b = b;
    }
    public Integer call() throws Exception {
        Thread.sleep((long)(Math.random() * 15000));
        return a + b;
    }
}
```

```java
ExecutorService executor = Executors.newFixedThreadPool(5);
List<Future<Integer>> list = new ArrayList<Future<Integer>>();
for (int i = 1; i < 11; i = i + 2) {
    Calculator c = new Calculator(i, i + 1);
    list.add(executor.submit(c));
}
int s = 0;
for (Future<Integer> f : list) {
    try {
        s = s + f.get();
    } catch (Exception e) {}
}
executor.shutdown();
```

**Rilevanza per il progetto**: mostra il pattern completo `ExecutorService` + `submit()` + `Future.get()` — direttamente riusabile per il **server multithreaded con thread pooling** richiesto dalle specifiche. Il pool di thread server-side che gestisce le connessioni client seguirà una struttura analoga (anche se probabilmente con `Runnable`/`execute()` se non serve un valore di ritorno esplicito, dato che la comunicazione avviene tramite socket).

### Esempio di race condition (da NON riprodurre, ma da capire)

```java
class Cell {
    private long value;
    public Cell(long v) { this.value = v; }
    public void update(long delta) {
        this.value += delta;  // NON atomico!
    }
    public long get() { return value; }
}
```

**Rilevanza per il progetto**: esempio pedagogico di ciò che va **evitato**. Qualunque struttura dati condivisa nel server (es. lo stato di una partita "Connections" acceduta da più thread client) deve evitare questo pattern non protetto.

### Metodo sincronizzato (fix della race condition)

```java
class Cell {
    private long value;
    public Cell(long v) { this.value = v; }
    public synchronized void update(long delta) {
        this.value += delta;
    }
    public long get() { return value; }
}
```

**Rilevanza per il progetto**: pattern minimo di thread-safety da applicare a qualsiasi classe condivisa nel server (es. una classe che tiene traccia del punteggio o dello stato di una board di gioco acceduta da più connessioni client concorrenti).

### Coda produttore-consumatore con monitor (`wait`/`notifyAll`)

```java
public class MessageQueue {
    int putptr, takeptr, count;
    final Object[] items;

    public MessageQueue(int size) {
        items = new Object[size];
        count = 0; putptr = 0; takeptr = 0;
    }

    public synchronized void produce(Object x) {
        while (count == items.length)
            try { wait(); } catch (Exception e) {}
        items[putptr] = x; putptr++; ++count;
        if (putptr == items.length) putptr = 0;
        notifyAll();
    }

    public synchronized Object consume() {
        while (count == 0)
            try { wait(); } catch (InterruptedException e) {}
        Object data = items[takeptr]; takeptr = takeptr + 1; --count;
        if (takeptr == items.length) { takeptr = 0; }
        notifyAll();
        return data;
    }
}
```

**Rilevanza per il progetto**: questo è **il pattern più direttamente riusabile** dell'intera lezione per Connections. Una coda circolare thread-safe di questo tipo è esattamente ciò che serve per:
- disaccoppiare il thread che riceve pacchetti UDP (notifiche asincrone) dal thread che li elabora,
- gestire code di richieste in ingresso al server multithreaded prima che vengano assegnate ai thread del pool,
- sincronizzare l'accesso a strutture dati di gioco condivise tra client diversi.

Da notare il pattern **`while (condizione) wait();`** (non `if`): è la forma corretta e va sempre preferita a un semplice `if`, per gestire correttamente i risvegli spuri e le race tra più consumatori/produttori.

---

## 3. Integrazione di Codice Esterno

I seguenti elementi della Standard Library **non** compaiono nel file caricato ma sono fortemente raccomandati per il progetto Connections, come alternativa o completamento moderno ai meccanismi di sincronizzazione "classici" (`synchronized`/`wait`/`notify`) qui presentati.

### `java.util.concurrent.BlockingQueue` (es. `LinkedBlockingQueue`)

La lezione implementa manualmente una coda produttore-consumatore con `wait`/`notifyAll`. La libreria standard offre già un'implementazione pronta, thread-safe, meno soggetta ad errori:

```java
BlockingQueue<String> notificheUDP = new LinkedBlockingQueue<>();
// producer (thread che riceve pacchetti UDP):
notificheUDP.put(messaggio); // si blocca se la coda è piena (se a capacità limitata)
// consumer (thread che processa le notifiche):
String msg = notificheUDP.take(); // si blocca se la coda è vuota
```

Necessaria/fortemente consigliata per gestire in modo sicuro e conciso il flusso tra thread che ricevono notifiche UDP e thread che le processano, evitando di reimplementare a mano il pattern `wait`/`notifyAll` visto nella lezione.

### `java.util.concurrent.locks.ReentrantLock` e `Condition`

Alternativa esplicita alle lock intrinseche, utile quando serve maggiore flessibilità (es. `tryLock()` con timeout, lock non annidate al blocco del metodo, più `Condition` distinte sulla stessa lock).

```java
ReentrantLock lock = new ReentrantLock();
Condition nonVuota = lock.newCondition();
lock.lock();
try {
    while (coda.isEmpty()) nonVuota.await();
    // ...
} finally {
    lock.unlock();
}
```

Utile se la logica del server richiede condizioni di attesa multiple e distinte sulla stessa risorsa (es. "coda piena" vs "partita terminata"), cosa che con un singolo monitor intrinseco (`wait`/`notifyAll` indifferenziato) è più scomoda da gestire.

### `java.util.concurrent.ConcurrentHashMap`

Per strutture dati condivise chiave-valore (es. mappa `id_partita → stato_partita`, o `client → sessione`), `ConcurrentHashMap` offre thread-safety con performance migliori rispetto a un `HashMap` sincronizzato manualmente con `synchronized`.

```java
ConcurrentHashMap<String, GameSession> partiteAttive = new ConcurrentHashMap<>();
partiteAttive.put(idPartita, nuovaSessione);
GameSession sessione = partiteAttive.get(idPartita);
```

Necessaria per gestire in modo sicuro ed efficiente lo stato condiviso di più partite/sessioni client accedute concorrentemente dal thread pool del server, evitando di sincronizzare l'intera struttura dati (con conseguente collo di bottiglia) come farebbe un `synchronized` su un `HashMap` classico.

---
# Lezione 4

### Il problema della thread-safety nelle collezioni Java

Il file affronta un tema di importanza cruciale per il progetto **Connections**: la gestione sicura di strutture dati condivise tra thread concorrenti. Poiché il server dovrà essere **multithreaded** (gestione dei client tramite thread pooling), qualunque struttura dati condivisa tra i thread — ad esempio le sessioni di gioco attive, i punteggi, le connessioni client, le code di notifiche — deve essere protetta da accessi concorrenti non sincronizzati.

**Collezioni non thread-safe**
`HashMap`, `ArrayList`, `LinkedList` non offrono alcuna garanzia di sincronizzazione. Operazioni apparentemente semplici come `add()` sono in realtà composte da più passi elementari (calcolo dimensione, inserimento, aggiornamento contatore), quindi due `add()` concorrenti possono corrompere lo stato interno della struttura.

**Tre strategie per garantire la thread-safety**

| Approccio | Meccanismo | Caratteristiche |
|---|---|---|
| Thread-safe "storiche" | `Vector`, `Hashtable` | Sincronizzano ogni metodo con lock intrinseca sull'intero oggetto |
| Synchronized Collections | `Collections.synchronizedList/Map/...` | Wrapper che incapsula ogni metodo in blocco `synchronized`; **conditionally thread-safe** |
| Concurrent Collections | `java.util.concurrent.*` (es. `ConcurrentHashMap`, `BlockingQueue`) | **Fine-grained locking**, iteratori fail-safe, operazioni atomiche composte |

Un punto teoricamente rilevante per l'esame orale: le *synchronized collections* sono **"conditionally thread-safe"** — le singole operazioni sono atomiche, ma sequenze di operazioni (es. `isEmpty()` seguito da `remove(0)`) non lo sono automaticamente, e vanno protette esplicitamente con blocchi `synchronized(oggetto)`.

**ConcurrentHashMap: il cuore della lezione**
Il vantaggio prestazionale (dimostrato dai benchmark: ~377ms contro ~1400ms di `Hashtable`/`SynchronizedMap`) deriva dal **lock striping**: la mappa è divisa in segmenti, ciascuno con una propria lock, permettendo scritture concorrenti su segmenti diversi. Inoltre offre operazioni atomiche composte (`putIfAbsent`, `remove(K,V)`, `replace(K,V,V)`) che risolvono race condition tipiche del pattern "query-then-update" (esempio: il metodo `getOrCreate` mostrato nel file, non atomico se implementato con `get()`+`put()` separati).

### Collegamento diretto alle specifiche del progetto Connections

Questi concetti sono **direttamente applicabili** al server multithreaded del progetto:
- Il **pool di thread** del server (uno per client, o gestito da thread pooling) accederà concorrentemente a strutture condivise (es. mappa `sessionId → GameSession`, elenco client connessi, contatori di punteggio).
- Le **notifiche asincrone via UDP** implicano che un thread separato possa dover leggere/scrivere dati aggiornati da altri thread che gestiscono la logica di gioco: serve quindi una collezione thread-safe o una sincronizzazione esplicita.
- La struttura a **ConcurrentHashMap** è il candidato naturale per mappare, ad esempio, gli ID di sessione/partita agli oggetti che rappresentano lo stato del gioco.

## 2. Codice Utile dal File

### Wrapper sincronizzato (utile come alternativa più semplice, se il grado di concorrenza è basso)

```java
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<Integer>());
```

*Rilevanza:* utile se nel progetto si opta per una struttura condivisa più semplice (es. lista dei client connessi) dove il livello di concorrenza è contenuto e non giustifica la complessità di una concurrent collection dedicata. **Attenzione**: se si effettuano operazioni composte (es. iterazione), va comunque racchiuso in un blocco `synchronized` sull'oggetto stesso.

### ConcurrentHashMap con operazioni atomiche

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface ConcurrentMap<K,V> extends Map<K,V> {
    V putIfAbsent(K key, V value);
    boolean remove(K key, V value);
    boolean replace(K key, V oldValue, V newValue);
    V replace(K key, V newValue);
}
```

*Rilevanza:* candidato diretto per gestire strutture condivise nel server (es. `ConcurrentHashMap<String, ClientSession>` per associare ID client alle rispettive sessioni). `putIfAbsent` è particolarmente utile per evitare race condition nella creazione di sessioni/partite quando più thread potrebbero tentare di registrare la stessa chiave contemporaneamente.

### Pattern corretto per operazioni composte protette da blocco sincronizzato

```java
synchronized (obj) {
    while (!condition) {
        try {
            obj.wait();
        } catch (InterruptedException ex) {
            // gestione interruzione
        }
    }
}

// altrove, per segnalare la condizione
synchronized (obj) {
    condition = ...;
    obj.notifyAll();
}
```

*Rilevanza:* questo pattern (wait/notify all'interno di blocchi sincronizzati, con verifica della condizione **in un ciclo `while`**, non `if`) è essenziale per l'implementazione di meccanismi di sincronizzazione custom — ad esempio se il server deve attendere che tutti i giocatori di una partita si siano connessi prima di avviare il game loop, o per implementare code di attesa personalizzate.

*Nota di integrazione:* la "regola d'oro" del `while` (invece di `if`) va sempre rispettata per evitare *spurious wakeup* o condizioni rivalidate da altri thread nel frattempo.

## 3. Integrazione di Codice Esterno

Il file introduce le basi teoriche ma non mostra ancora alcune classi della Standard Library che saranno probabilmente necessarie per il progetto Connections, in particolare per la gestione del thread pool lato server e delle comunicazioni asincrone.

### `java.util.concurrent.BlockingQueue<E>` (interfaccia) e `LinkedBlockingQueue<E>` (implementazione)

Necessaria per implementare pattern **producer-consumer**, tipicamente usati nella gestione di un thread pool o nel disaccoppiamento tra ricezione di richieste (via NIO) e loro elaborazione da parte dei worker thread.

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

// produttore
taskQueue.put(nuovoTask);

// consumatore (worker thread)
Runnable task = taskQueue.take(); // si blocca finché non c'è un elemento
task.run();
```

### `java.util.concurrent.ExecutorService` e `Executors` (package `java.util.concurrent`)

Anche se solo citato di sfuggita nel file (nei benchmark), è la API standard per la gestione di un **thread pool**, requisito esplicito del server nel progetto Connections.

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService executorService = Executors.newFixedThreadPool(10);
executorService.execute(() -> gestisciClient(socketClient));

// alla chiusura del server
executorService.shutdown();
executorService.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.SECONDS);
```

### `java.util.concurrent.atomic.AtomicInteger` / `AtomicLong`

Citato nella slide come parte del framework (`java.util.concurrent.atomic`) ma non approfondito nel file. Utile per contatori condivisi (es. numero di partite attive, ID progressivi di sessione) senza dover ricorrere a blocchi `synchronized` espliciti, garantendo operazioni atomiche a costo inferiore.

```java
import java.util.concurrent.atomic.AtomicInteger;

AtomicInteger sessionCounter = new AtomicInteger(0);
int nuovoId = sessionCounter.incrementAndGet(); // operazione atomica
```

*Perché è necessario:* per generare identificatori univoci di sessione/partita in modo thread-safe senza overhead di lock espliciti, scenario molto comune in un server multithreaded come quello richiesto dal progetto.
---
# Lezione 5

## 1. Teoria

### Il paradigma Client/Server e i protocolli di trasporto

Due processi (non thread) in esecuzione su host diversi comunicano e cooperano utilizzando protocolli — insiemi di regole condivise. Il corso utilizza due protocolli di livello trasporto:

- **connection-oriented (TCP)**: come una chiamata telefonica, stabilisce un **canale di comunicazione dedicato** tra mittente e destinatario → **stream socket**;
- **connectionless (UDP)**: come l'invio di una lettera, ogni messaggio è instradato indipendentemente dagli altri, senza canale dedicato → **datagram socket**.

In `java.net`, la programmazione TCP è **asimmetrica** (classe `Socket` lato client, classi `ServerSocket` + `Socket` lato server), mentre quella UDP è **simmetrica** (`DatagramSocket` + `DatagramPacket` per entrambi i lati).

**Rilevanza per il progetto**: le specifiche di Connections richiedono esattamente entrambi i modelli — **TCP persistente** per il canale client-server principale, e **UDP** per le notifiche asincrone. Questa lezione fornisce le basi per la parte TCP lato client (la parte server sarà trattata in lezioni successive con `ServerSocket`).

### La classe `InetAddress`

Incapsula in un unico oggetto sia l'indirizzo IP numerico (`byte[] address`) sia il nome di dominio associato. **Non ha costruttori pubblici**: si costruisce tramite **factory methods statici**, alcuni dei quali contattano il DNS (e possono sollevare `UnknownHostException`):

- `getByName(String hostname)` — risolve un singolo host;
- `getAllByName(String hostname)` — risolve tutti gli indirizzi associati a un host;
- `getLocalHost()` — indirizzo della macchina locale;
- `getByAddress(byte[])` / `getByAddress(String, byte[])` — costruiscono l'oggetto **senza** contattare il DNS (utile se il DNS non è raggiungibile).

Importante: questi metodi effettuano **caching** delle risoluzioni (di default: per sempre le risoluzioni riuscite, 10 secondi quelle fallite), controllabile con `Security.setProperty("networkaddress.cache.ttl", ...)`.

**Rilevanza per il progetto**: se il client Connections deve connettersi a un server identificato per nome (piuttosto che per IP fisso), la risoluzione va fatta **una sola volta** con `InetAddress.getByName()` prima di aprire la connessione, non ripetutamente — esattamente come mostrato nell'analisi del `PortScanner`, dove risolvere l'host una volta anziché per ogni tentativo di connessione riduce drasticamente i tempi.

### Identificare un servizio: indirizzo IP + porta

Un servizio in rete è identificato da: rete + host (indirizzo IP) + processo sull'host (**porta**, intero 0–65535; 1–1023 riservate ai well-known services). Una comunicazione TCP è completamente individuata da una **5-upla**: `{protocollo, IP locale, porta locale, IP remoto, porta remota}`.

**Rilevanza per il progetto**: il file di configurazione richiesto dalle specifiche (per i parametri di client e server) dovrà includere host e porta del server; questa 5-upla è concettualmente ciò che il protocollo applicativo di Connections dovrà gestire per ogni connessione client-server.

### Socket lato client e stream

Il client apre un `Socket` specificando host e porta:

```java
public Socket(InetAddress host, int port) throws IOException
public Socket(String host, int port) throws UnknownHostException, IOException
```

Una volta stabilita la connessione, essa è modellata come **stream**: bidirezionale ma **asimmetrico** — occorrono due stream distinti, uno per leggere (`InputStream`) e uno per scrivere (`OutputStream`), ottenuti con `socket.getInputStream()` / `socket.getOutputStream()`.

Caratteristiche generali degli stream Java rilevanti:
- accesso **sequenziale**, ordine **FIFO**;
- **one way** (o solo lettura o solo scrittura);
- **bloccanti**: un'operazione di lettura/scrittura sospende il thread fino al completamento;
- **non c'è corrispondenza 1:1** tra scritture e letture: una singola `write()` di 100 byte può essere letta con più `read()` parziali dall'altra estremità — punto **cruciale** per il protocollo applicativo, che deve gestire il framing dei messaggi.

**Rilevanza per il progetto**: questo è **il meccanismo di base** su cui poggia (in alternativa a NIO) l'intera comunicazione client-server di Connections se si opta per socket bloccanti classici. Anche usando NIO, il concetto di "nessuna corrispondenza garantita tra write e read" resta valido e motiva l'uso dei buffer visto nella Lezione 8.

### `InputStreamReader`: da byte a caratteri Unicode

Gli stream di base leggono/scrivono **byte**. Per gestire testo, si usa `InputStreamReader` (wrapper che traduce byte in caratteri Unicode secondo una codifica specificata, es. `"ASCII"`, `"UTF-8"`) e il simmetrico `OutputStreamWriter` per la direzione opposta.

### Half-closed sockets e timeout

- `shutdownInput()` / `shutdownOutput()`: chiudono il socket in **una sola direzione** (utile nei protocolli richiesta-risposta: il client scrive la richiesta, chiude l'output, poi legge la risposta).
- `setSoTimeout(ms)`: imposta un timeout sulle operazioni di lettura, sollevando `SocketTimeoutException` (sottoclasse di `IOException`) se il server non risponde — **essenziale** per evitare attese indefinite.

**Rilevanza per il progetto**: `setSoTimeout()` è particolarmente importante per il client Connections, per evitare che il programma resti bloccato indefinitamente se il server non risponde (es. per rilevare disconnessioni). `shutdownOutput()` può essere utile se il protocollo applicativo prevede fasi request/response nette.

---

## 2. Codice Utile dal File

### Risoluzione di un hostname (`getByName`)

```java
InetAddress address = InetAddress.getByName("www.unipi.it");
System.out.println(address);
```

**Rilevanza**: pattern base per risolvere l'indirizzo del server Connections a partire dal nome host, se il file di configurazione del progetto specifica un hostname anziché un IP numerico.

### Creazione socket e stream, lettura testuale (client QOTD)

```java
Socket socket = new Socket(host, port);
BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
String line;
while ((line = in.readLine()) != null) {
    System.out.println(line);
}
socket.close();
```

**Rilevanza per il progetto**: è **il pattern client TCP più direttamente riusabile** per Connections — apertura socket, wrapping dello stream con `BufferedReader`/`InputStreamReader` per leggere righe di testo (utile se il protocollo applicativo scambia messaggi JSON/testo riga per riga). Da notare la corretta gestione della chiusura del socket in un blocco separato con try/catch.

### Ottenere informazioni su un socket connesso

```java
Socket theSocket = new Socket(host, 80);
System.out.println("Connected to " + theSocket.getInetAddress()
    + " on port " + theSocket.getPort() + " from port "
    + theSocket.getLocalPort() + " of " + theSocket.getLocalAddress());
```

**Rilevanza**: utile per **logging/debug** del client Connections — verificare a quale indirizzo/porta remota ci si è effettivamente connessi e quale porta locale (effimera) è stata assegnata, informazione preziosa in fase di sviluppo e test del protocollo di rete.

### Timeout sul socket e lettura carattere per carattere (client Daytime)

```java
socket = new Socket(hostname, 13);
socket.setSoTimeout(15000);
InputStream in = socket.getInputStream();
StringBuilder time = new StringBuilder();
InputStreamReader reader = new InputStreamReader(in, "ASCII");
for (int c = reader.read(); c != -1; c = reader.read()) {
    time.append((char) c);
}
```

```java
} catch (IOException ex) {
    System.out.println("could not connect to time.nist.gov");
} finally {
    if (socket != null) {
        try { socket.close(); } catch (IOException ex) { /* ignore */ }
    }
}
```

**Rilevanza per il progetto**: pattern **essenziale** — `setSoTimeout()` per evitare blocchi indefiniti sul client Connections in caso di server non responsivo, e la struttura `try/catch/finally` con chiusura garantita del socket è la forma robusta e corretta da adottare (in alternativa al try-with-resources, anch'esso mostrato più avanti nella lezione con `shutdownOutput`).

### Costruzione socket senza connessione immediata (con bind)

```java
Socket socket = new Socket();
SocketAddress address = new InetSocketAddress("time.nist.gov", 13);
socket.bind(address);
```

**Rilevanza**: pattern utile se il client Connections deve impostare opzioni sul socket (es. timeout, buffer size) **prima** di stabilire la connessione, cosa non possibile con il costruttore che connette immediatamente.

---

## 3. Integrazione di Codice Esterno

I seguenti elementi della Standard Library **non** compaiono nel file caricato ma sono necessari o fortemente raccomandati per completare la parte client della comunicazione TCP di Connections.

### `java.net.Socket#connect(SocketAddress, int timeout)`

Il file mostra `setSoTimeout()` per i timeout in lettura, ma non il timeout sulla fase di **connessione** stessa. Per evitare che il client resti bloccato a tempo indefinito se il server non è raggiungibile:

```java
Socket socket = new Socket();
socket.connect(new InetSocketAddress(host, port), 5000); // timeout 5s sulla connect
```

Necessario per rendere il client Connections robusto rispetto a server irraggiungibili o di rete lenta, complementando `setSoTimeout()` (che copre solo le `read()` successive alla connessione).

### `java.io.PrintWriter` (con `autoFlush`)

Il file mostra `OutputStreamWriter` per scrivere testo grezzo, ma per inviare comandi testuali/JSON riga per riga in modo comodo, `PrintWriter` con autoflush è lo standard de facto:

```java
PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // autoFlush=true
out.println(comandoJson); // invia e fa flush automaticamente
```

Fortemente raccomandato per l'invio dei comandi del protocollo applicativo del client Connections, evitando di dimenticare `flush()` esplicite dopo ogni `write()`.

### `java.net.SocketException` (gestione mirata)

Il file gestisce genericamente `IOException`, ma per distinguere un errore di connessione rifiutata da altri problemi di I/O è utile intercettare `SocketException` (sottoclasse di `IOException`, come mostrato nella gerarchia della Lezione 8, ma non usata esplicitamente qui):

```java
try {
    Socket socket = new Socket(host, port);
} catch (SocketException e) {
    // connessione rifiutata / porta chiusa
} catch (IOException e) {
    // altri errori di I/O
}
```

Utile per fornire messaggi diagnostici distinti nel client Connections (es. "server non in ascolto" vs. "errore di rete generico").

### `java.net.Socket#setTcpNoDelay(boolean)`

Non presente nel file. Per applicazioni interattive a bassa latenza come un gioco multiplayer (Connections invia comandi/notifiche in tempo reale), disabilitare l'algoritmo di Nagle evita ritardi introdotti dal buffering TCP di default:

```java
socket.setTcpNoDelay(true); // invia i pacchetti immediatamente, senza attese di batching
```

Consigliato per il client Connections per ridurre la latenza percepita nello scambio di comandi/risposte di gioco su connessione TCP persistente.
----
# Lezione 6 

## 1. L'astrazione dello Stream

Uno **stream** rappresenta un flusso sequenziale di dati che scorre da una sorgente a una destinazione. Le proprietà fondamentali sono:
- **accesso sequenziale**
- **ordinamento FIFO**
- **one-way** (un singolo verso)
- **bloccante**

Questa astrazione è centrale per il progetto Connections: la connessione TCP persistente tra client e server sarà gestita proprio attraverso stream (lato server, in combinazione con NIO come richiesto in sezione 3 delle specifiche).

## 2. Gerarchia `java.io`

Il package distingue due famiglie di stream, entrambe derivate da `Object`:

| Orientamento | Input | Output |
|---|---|---|
| **Byte** (dati grezzi, non strutturati) | `InputStream` (astratta) | `OutputStream` (astratta) |
| **Caratteri** (dati testuali) | `Reader` (astratta) | `Writer` (astratta) |

Gli stream orientati ai byte leggono/scrivono byte senza traduzione (adatti a immagini, video); quelli orientati ai caratteri traducono tra il formato interno Java (UTF-16) e la codifica esterna usata per memorizzazione/trasmissione.

## 3. Il problema della codifica: Mojibake

Un punto critico, particolarmente rilevante per il progetto: quando si apre una `Socket`, si ottengono `InputStream`/`OutputStream` **in byte**. Poiché Connections scambia messaggi **testuali in formato JSON**, è necessario convertire questi stream in `Reader`/`Writer` tramite le classi ponte `InputStreamReader` e `OutputStreamWriter`, specificando esplicitamente la codifica (es. `StandardCharsets.UTF_8`).

L'esempio del *mojibake* mostra cosa accade se lettura e scrittura usano codifiche diverse (`UTF_8` vs `ISO_8859_1`): i caratteri vengono corrotti. **Implicazione diretta per il progetto**: nel client NIO e nel server multithreaded, la codifica dei messaggi JSON scambiati sulla connessione TCP deve essere concordata e coerente tra le due parti (tipicamente UTF-8).

## 4. Il pattern "Decorator" dei filtri

`java.io` adotta un approccio **a livelli**: stream di base (es. `FileInputStream`) possono essere "avvolti" da filtri che aggiungono funzionalità:
- **buffering** (`BufferedInputStream`/`BufferedOutputStream`) — bufferizza i dati a blocchi anziché byte per byte, con guadagni prestazionali significativi (l'esempio in slide mostra una riduzione da ~147 ms a ~11 ms nella copia di un file);
- **traduzione a formato strutturato** (`DataInputStream`/`DataOutputStream`) — permette di leggere/scrivere tipi primitivi in binario;
- **conversione byte↔caratteri** (`InputStreamReader`/`OutputStreamWriter`).

I filtri sono **concatenabili** (chaining), ad esempio:
```
FileInputStream → BufferedInputStream → DataInputStream
```
Questo pattern (Decorator) è utile concettualmente per capire come strutturare, lato server, la lettura dei messaggi in arrivo sulla connessione TCP.

## 5. La classe `File`

Un'istanza di `File` **non è il file stesso**, ma un descrittore del path, con metodi per verificarne l'esistenza, il tipo (`isDirectory()`), elencarne il contenuto (`list()`), ecc. È rilevante per l'Assignment 6 (elaborazione di liste di directory).

## 6. `try-with-resources`

Costrutto (Java 7, aggiornato in Java 9) per la chiusura automatica e sistematica delle risorse che implementano `AutoCloseable` (file, stream, reader, **socket**). Punti chiave:
- le risorse dichiarate nella clausola `try(...)` vengono chiuse automaticamente all'uscita dal blocco, in **ordine inverso** rispetto alla dichiarazione;
- risolve il problema delle **suppressed exceptions**: se un'eccezione viene sollevata sia nel blocco `try` sia durante la `close()` implicita, la prima "vince" e viene propagata, mentre la seconda viene soppressa (accessibile comunque tramite `getSuppressed()`), evitando la perdita di informazione tipica del pattern `finally` classico.

**Rilevanza per il progetto**: sia il client (gestione della connessione NIO/Socket, file di configurazione) sia il server (gestione delle connessioni multiple, file di persistenza JSON) beneficiano di questo costrutto per garantire la chiusura corretta delle risorse anche in presenza di eccezioni — un requisito implicito di robustezza per un'applicazione client-server persistente.

---

## Codice Utile dal File

### 1. Apertura di una connessione TCP con conversione a Reader/Writer e codifica esplicita

```java
try (Socket socket = new Socket(host, port)) {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
    );
    BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
    );
    // ...
} catch (IOException e) {
    e.printStackTrace();
}
```
**Perché è rilevante**: è lo schema base per scambiare messaggi testuali JSON su una connessione TCP, esattamente il meccanismo richiesto in sezione 3 delle specifiche del progetto (comandi client→server in formato JSON su connessione TCP persistente). **Nota di integrazione**: nella slide originale il client usa deliberatamente `UTF_8` per il reader e `ISO_8859_1` per il writer per dimostrare il mojibake; per Connections andrà usata la **stessa codifica** (tipicamente UTF-8) su entrambi i lati.

### 2. `try-with-resources` con risorse multiple concatenate

```java
try (FileInputStream input = new FileInputStream(new File("immagine.jpg"));
     BufferedInputStream bufferedInput = new BufferedInputStream(input)) {
    int data = bufferedInput.read();
    while (data != -1) {
        System.out.print((char) data);
        data = bufferedInput.read();
    }
}
```
**Perché è rilevante**: mostra la sintassi per dichiarare più risorse concatenate in un unico blocco — pattern applicabile, ad esempio, alla lettura dei file JSON di persistenza (`FileInputStream` + eventuale `BufferedInputStream`) nel `PersistenceManager` già implementato per la Macro-2.

### 3. Uso della classe `File` per iterare i contenuti di una directory

```java
File dir = new File(".");
if (dir.isDirectory()) {
    String[] files = dir.list();
    for (String file : files) {
        if (file.endsWith(".java"))
            System.out.println(file);
    }
}
```
**Nota di integrazione/contesto**: questo è il pattern richiesto dall'**Assignment 6** (comprimere con gzip tutti i file contenuti in una lista di directory, senza ricorsione sulle sottodirectory nel caso base). Andrà combinato con un `ExecutorService` (thread pool) per eseguire la compressione di ogni file come task separato — coerente con il requisito di server multithreaded basato su thread pooling del progetto Connections.

---

## Integrazione di Codice Esterno

### 1. `java.util.zip.GZIPOutputStream` — necessario per l'Assignment 6

Il file non menziona alcuna classe di supporto per la compressione gzip, che è invece l'oggetto esplicito dell'Assignment 6. La classe standard adatta è:

```java
import java.util.zip.GZIPOutputStream;
import java.io.*;

try (FileInputStream fis = new FileInputStream(sourceFile);
     FileOutputStream fos = new FileOutputStream(sourceFile.getName() + ".gz");
     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

    byte[] buffer = new byte[8192];
    int len;
    while ((len = fis.read(buffer)) != -1) {
        gzos.write(buffer, 0, len);
    }
}
```
`GZIPOutputStream` estende `DeflaterOutputStream` e implementa esattamente il pattern "filtro che avvolge uno stream di base" visto a lezione, applicato alla compressione anziché al buffering.

### 2. `java.util.concurrent.ExecutorService` / `Executors` — necessario per eseguire i task nel thread pool

Il file introduce il concetto di thread pooling solo a livello di requisito testuale ("task eseguito in un threadpool"), senza fornire l'API. La libreria standard Java offre:

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService pool = Executors.newFixedThreadPool(4);
for (File f : filesToCompress) {
    pool.submit(() -> compressFile(f));  // task di compressione gzip
}
pool.shutdown();
```
Questo è direttamente il meccanismo richiesto sia dall'Assignment 6 sia — più in generale — dal server Connections ("server multithreaded realizzato usando Java thread pooling", sezione 3 delle specifiche).

### 3. `java.nio.file.Files` / `java.nio.file.Paths` — alternativa moderna a `File` per l'enumerazione dei contenuti di una directory

Il file usa l'API storica `File.list()`. La libreria standard offre un'alternativa più moderna e con gestione delle eccezioni più esplicita, utile in particolare se si volesse affrontare la parte facoltativa ricorsiva dell'Assignment 6:

```java
import java.nio.file.*;
import java.util.stream.Stream;

try (Stream<Path> paths = Files.list(Paths.get(directoryPath))) {
    paths.filter(Files::isRegularFile)
         .forEach(p -> pool.submit(() -> compressFile(p.toFile())));
}
```
`Files.list()` restituisce uno `Stream<Path>` (non ricorsivo, adatto al caso base dell'assignment), mentre `Files.walk()` sarebbe l'analogo per la versione ricorsiva facoltativa.

---

Puoi caricare il prossimo file (lezione, assignment o esempio di codice) quando vuoi procedere.

---
# Lezione 7 - Socket lato server: architettura a due socket

Il documento introduce la distinzione fondamentale, lato server, tra due tipologie di socket TCP:

- **welcome (passive/listening) socket**: rappresentato in Java dalla classe `ServerSocket`, è associato ad una porta nota e ha il solo compito di accettare le richieste di connessione in arrivo.
- **connection (active) socket**: rappresentato dalla classe `Socket`, viene creato automaticamente dal server (tramite `accept()`) per ogni client che si connette, e costituisce l'endpoint effettivo attraverso cui avviene lo scambio di byte (stream-based communication).

Questo modello a due socket è **direttamente rilevante per la componente server di Connections**, dato che le specifiche richiedono un server multithreaded che gestisca connessioni TCP persistenti con più client contemporaneamente: ogni `accept()` produce un nuovo `Socket` dedicato, che il server dovrà mantenere aperto per tutta la durata della sessione di gioco del client.

## Ciclo di vita del server e comunicazione stream-based

Il ciclo di vita tipico è: istanziare `ServerSocket` → loop `while(true)` con `accept()` bloccante → ottenere `InputStream`/`OutputStream` dal socket restituito → comunicare → chiudere il socket. Va notato che `accept()` è bloccante: se non ci sono richieste, il thread si sospende (possibile gestione tramite timeout).

## Server multithreaded e Thread Pooling

Il punto più rilevante per il progetto è il passaggio dal modello "un thread per client" (semplice ma con costo in risorse, es. 1MB di RAM per thread × 1000 thread) al **thread pooling** tramite `ExecutorService`. Le specifiche di Connections richiedono esplicitamente un "server multithreaded con Java thread pooling": il pattern mostrato nel file (`Executors.newFixedThreadPool(n)` + classe `ClientHandler implements Runnable` + `pool.execute(...)`) è **esattamente l'architettura da replicare** per il server di Connections, sostituendo la logica del servizio "quote of the day" con la logica di gestione delle partite/lobby del gioco.

Un dettaglio architetturale importante: nel modello con thread pool, l'`accept()` avviene **fuori** dal thread eseguito nel pool (nel thread principale del server), mentre la gestione della singola connessione (lettura/scrittura, elaborazione) viene delegata al worker thread. Questo disaccoppia l'accettazione di nuove connessioni dal servizio delle connessioni già stabilite — un requisito implicito per un server che deve rimanere reattivo a nuovi client durante partite già in corso.

## UDP e DatagramSocket/DatagramPacket

La seconda parte della lezione è **direttamente applicabile alle "notifiche asincrone via UDP"** richieste dalle specifiche di Connections. I concetti chiave:

- UDP è **connectionless**: non serve stabilire una connessione, ma occorre specificare destinatario (IP+porta) per ogni singolo pacchetto (`DatagramPacket`).
- La comunicazione è "timely rather than orderly and reliable": nessuna garanzia di ordine o consegna — coerente con l'uso tipico delle notifiche (dove la perdita occasionale di un pacchetto è tollerabile, a differenza dei comandi di gioco su TCP).
- `send()` è non bloccante, `receive()` è bloccante (gestibile con `setSoTimeout`).
- Il pattern client/server è simmetrico: entrambi usano `DatagramSocket`, a differenza di TCP dove client e server usano classi diverse (`Socket` vs `ServerSocket`).

Un punto critico da tenere presente per l'implementazione delle notifiche: la **dimensione massima** di un `DatagramPacket` è teoricamente 65536 byte, ma è spesso limitata a 8192 byte dal sistema operativo (per sicurezza si consiglia di restare sotto i 512 byte). Questo va considerato nel progettare il formato (presumibilmente JSON, vista la scelta GSON del corso) delle notifiche asincrone, che dovranno essere payload compatti.

# Codice Utile dal File

## 1. Server multithreaded con Thread Pool — schema architetturale di riferimento

```java
try (ServerSocket serverSocket = new ServerSocket(port)) {
    System.out.println("Server attivo sulla porta " + port);
    ExecutorService pool = Executors.newFixedThreadPool(20);
    while (true) {
        Socket clientSocket = serverSocket.accept();
        pool.execute(new ClientHandler(clientSocket, /* stato condiviso */));
    }
} catch (IOException e) {
    System.err.println("Errore di IO: " + e.getMessage());
}
```

**Rilevanza**: è lo scheletro esatto da adattare per il server TCP di Connections. La dimensione del pool (qui 20, fissa) andrà probabilmente resa configurabile tramite il file di configurazione richiesto dalle specifiche del progetto, e il costruttore di `ClientHandler` dovrà ricevere, oltre al socket, i riferimenti allo stato condiviso della partita/lobby (invece del semplice array di frasi `quotes`).

## 2. ClientHandler come Runnable — pattern del worker thread

```java
private static class ClientHandler implements Runnable {
    private Socket clientSocket;
    // altri campi di stato...

    public ClientHandler(Socket socket, /* ... */) {
        this.clientSocket = socket;
        // ...
    }

    public void run() {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            // logica di gestione della singola connessione
        } catch (IOException e) {
            System.err.println("Errore con il client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del client:" + e.getMessage());
            }
        }
    }
}
```

**Modifiche necessarie per il progetto**: nel caso di Connections la connessione TCP è **persistente** (non un singolo invio come nel QuoteServer), quindi il corpo del metodo `run()` dovrà contenere un ciclo di lettura continua dei comandi/messaggi del client (tipicamente leggendo da un `BufferedReader`/`InputStream` finché il client non si disconnette), non un singolo scambio "send-and-close". Va inoltre gestita con attenzione la sincronizzazione se `ClientHandler` accede a stato condiviso tra thread (es. lo stato di una partita multi-giocatore).

## 3. DatagramSocket e DatagramPacket per l'invio — pattern per le notifiche

```java
String daytime = new Date().toString();
byte[] data = daytime.getBytes("US-ASCII");
InetAddress host = request.getAddress();
int port = request.getPort();
DatagramPacket response = new DatagramPacket(data, data.length, host, port);
socket.send(response);
```

**Rilevanza**: schema diretto per l'invio di una notifica UDP asincrona verso un client. Nel contesto di Connections, `data` dovrà contenere il payload serializzato (presumibilmente JSON via GSON, come indicato dal materiale del corso), e l'indirizzo/porta del client destinatario dovranno essere noti al server (probabilmente registrati al momento dell'handshake TCP iniziale, dato che UDP è connectionless).

## 4. Costruzione del buffer di ricezione con offset/lunghezza corretti

```java
String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), "US-ASCII");
```

**Rilevanza e attenzione**: il file evidenzia esplicitamente (slide "Dati ricevuti") un errore comune — usare `getData()` senza specificare `offset` e `length` produce dati "sporchi" (il buffer non viene ridimensionato automaticamente, quindi contiene byte residui delle ricezioni precedenti). Questo è un dettaglio critico da rispettare quando si deserializzano le notifiche JSON ricevute via UDP nel client di Connections, per evitare errori di parsing.

# Integrazione di Codice Esterno

## `java.util.concurrent.ExecutorService` e `Executors` (metodi aggiuntivi)

Il file mostra solo `Executors.newFixedThreadPool(int)`. Per il progetto è utile conoscere anche:

- `ExecutorService.shutdown()` / `shutdownNow()`: necessari per una terminazione pulita del server (gestione del comando di arresto), assente nel materiale mostrato.

```java
ExecutorService pool = Executors.newFixedThreadPool(20);
// ... uso del pool ...
pool.shutdown(); // avvia una terminazione ordinata, senza accettare nuovi task
try {
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // forza la terminazione
    }
} catch (InterruptedException e) {
    pool.shutdownNow();
}
```

È necessario perché il server di Connections dovrà poter essere arrestato in modo controllato (es. da riga di comando o segnale), completando le partite in corso o notificando i client, invece di terminare bruscamente il processo.

## `java.io.BufferedReader` (con `InputStreamReader`)

Non presente nel file, che mostra solo `PrintWriter` per l'output. Per leggere comandi testuali (o righe JSON) dal client su una connessione TCP persistente è fortemente consigliato:

```java
BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
String line;
while ((line = in.readLine()) != null) {
    // elabora il comando ricevuto dal client
}
```

Necessario perché il modello di comunicazione della lezione (una singola risposta e chiusura) non copre il caso, centrale nel progetto, di un client che invia più comandi sulla stessa connessione persistente.

## `java.util.concurrent.ConcurrentHashMap` (package `java.util.concurrent`)

Non trattato nel file. Poiché più `ClientHandler` (eseguiti su thread diversi del pool) dovranno probabilmente accedere e modificare strutture dati condivise (es. mappa delle partite attive, mappa client→indirizzo UDP per le notifiche), è necessaria una struttura dati thread-safe:

```java
ConcurrentHashMap<String, GameSession> activeGames = new ConcurrentHashMap<>();
activeGames.put(gameId, new GameSession(...));
GameSession session = activeGames.get(gameId);
```

Necessario per evitare race condition sullo stato condiviso, un rischio non affrontato dalla lezione (che gestisce solo dati immutabili, l'array di frasi).

## `java.net.InetSocketAddress`

Non usato esplicitamente nel file (viene usato `InetAddress` + `int port` separati). È utile per la gestione più compatta e tipizzata dell'associazione IP+porta necessaria per instradare le notifiche UDP verso il client corretto:

```java
InetSocketAddress clientAddr = new InetSocketAddress(clientIp, clientUdpPort);
DatagramPacket notification = new DatagramPacket(data, data.length, clientAddr);
socket.send(notification);
```

Preferibile a gestire IP e porta come campi separati quando questa coppia deve essere memorizzata come chiave o valore in strutture dati (es. associare ogni giocatore TCP alla sua porta UDP per le notifiche).

---

Le specifiche complete del progetto Connections mi sono state fornite solo come sintesi in questa conversazione: se disponi del testo integrale (v1.15, gennaio 2026) e vuoi che io lo usi come riferimento più puntuale nei prossimi file, puoi condividerlo. Nel frattempo, carica pure il prossimo file quando sei pronto.
---

# Lezione 8 – Java Stream-Oriented I/O e Java NIO

### Java Stream-Oriented I/O vs. Java NIO

Prima di NIO, Java offriva solo I/O basato su **stream**: unidirezionali e bloccanti, con lettura/scrittura byte per byte (eventualmente bufferizzati tramite `BufferedInputStream`/`BufferedOutputStream`).

NIO (New I/O, introdotto in Java 1.4) introduce un modello alternativo basato su **Buffer** e **Channel**:

- Il **Channel** è l'interfaccia verso il dispositivo (file, socket) — è **bidirezionale** e può essere **non bloccante**.
- Il **Buffer** è l'interfaccia tra il programma e il Channel: il canale legge/scrive dati da/verso un buffer.

Questa distinzione è **centrale per il progetto Connections**: la specifica richiede che il **client gestisca la connessione TCP persistente in NIO**, quindi la comprensione di `SocketChannel` (visto solo di sfuggita in questa lezione, ma erede diretto di questa gerarchia) dipende interamente da questi concetti di base su `Buffer` e `Channel`.

### Il Buffer: variabili di stato

Un `Buffer` non è un semplice array: incapsula dati **e** stato, tramite quattro variabili sempre in relazione:

```
0 ≤ mark ≤ position ≤ limit ≤ capacity
```

| Variabile | Significato |
|---|---|
| **capacity** | dimensione massima, fissata alla creazione |
| **limit** | primo elemento che non deve essere letto/scritto |
| **position** | prossimo indice di lettura/scrittura |
| **mark** | posizione salvata, ripristinabile con `reset()` |

### Ciclo di vita: filling / draining

- **Filling mode**: il produttore scrive nel buffer (`put`), `position` avanza, `limit = capacity`.
- **Draining mode**: il consumatore legge (`get`), `position` avanza fino a `limit`.

Le transizioni tra i due stati sono gestite da metodi essenziali:

- **`flip()`**: passa da filling a draining (`limit = position`, `position = 0`). **Fondamentale**: va sempre chiamato prima di leggere ciò che si è appena scritto, o prima che un Channel legga dal buffer per scriverlo su rete/file.
- **`clear()`**: torna a filling mode senza cancellare i dati (`position = 0`, `limit = capacity`).
- **`compact()`**: come `clear()`, ma preserva i dati non ancora letti spostandoli in testa al buffer — indispensabile quando un messaggio non è stato letto per intero e si vuole continuare a scrivere (tipico in comunicazioni di rete dove i dati arrivano incrementalmente).
- **`rewind()`**: resetta `position = 0` senza toccare `limit`, per rileggere gli stessi dati.
- **`mark()` / `reset()`**: salvano e ripristinano una posizione.

Per il progetto, `compact()` è particolarmente rilevante: nel protocollo client-server via `SocketChannel` non bloccante, un messaggio potrebbe arrivare frammentato su più `read()`, e `compact()` è lo strumento corretto per non perdere i byte già letti ma non ancora "consumati" dal livello applicativo.

### Direct vs Non-Direct Buffer

- `ByteBuffer.allocate(n)`: buffer sullo heap JVM — comporta una **doppia copia** dei dati (kernel buffer → heap buffer).
- `ByteBuffer.allocateDirect(n)`: buffer allocato fuori dallo heap, con accesso diretto alla memoria del kernel — **migliori prestazioni** ma **maggior costo di allocazione/deallocazione** e non gestito dal garbage collector.

Questa distinzione è direttamente collegata all'**Assignment 8**, che chiede di confrontare empiricamente le prestazioni di diverse strategie di bufferizzazione (buffer diretti, indiretti, `transferTo()`, stream bufferizzati, byte-array manuale) al variare di dimensione del file e del buffer.

### Channel: gerarchia

`Channel` è l'interfaccia radice da cui derivano:

- `FileChannel` — I/O su file, bloccante, thread-safe
- `SocketChannel` — I/O su TCP, può essere non bloccante
- `ServerSocketChannel` — accetta connessioni TCP e genera `SocketChannel`
- `DatagramChannel` — I/O su UDP, può essere non bloccante

Per il progetto: il **server multithreaded** userà presumibilmente `ServerSocketChannel`/`SocketChannel` (o socket bloccanti classici con thread pool), mentre le **notifiche asincrone via UDP** richiederanno `DatagramChannel`. Questi canali non sono trattati in dettaglio in questa lezione (rimandati a lezioni successive sui Selector), ma la lezione fornisce le basi indispensabili sul funzionamento di `read()`/`write()` tramite buffer.

---

## 2. Codice Utile dal File

### Creazione e ispezione di un ByteBuffer

```java
ByteBuffer byteBuffer1 = ByteBuffer.allocate(10);
System.out.println(byteBuffer1);
// java.nio.HeapByteBuffer[pos=0 lim=10 cap=10]
```

**Rilevanza per il progetto**: questo pattern (allocare un buffer di dimensione fissa e monitorarne lo stato tramite `toString()`) sarà il mattone base per costruire il protocollo di comunicazione client-server. Utile in fase di debug per verificare che `position`/`limit` siano coerenti prima di ogni `read`/`write` sul canale.

### Scrittura/lettura di stringhe tramite Buffer (esempio "chat")

```java
ByteBuffer chatBuffer = ByteBuffer.allocate(128);
String messageFromAlice = "Hi Bob, how are you?";
chatBuffer.put(messageFromAlice.getBytes(StandardCharsets.UTF_8));
// Passaggio di stato: ora Bob deve leggere → flip()
chatBuffer.flip();
byte[] receivedBytes = new byte[chatBuffer.remaining()];
chatBuffer.get(receivedBytes);
String messageForBob = new String(receivedBytes, StandardCharsets.UTF_8);
// Bob risponde → clear() e scrive di nuovo
chatBuffer.clear();
```

**Rilevanza per il progetto**: è **il pattern più direttamente riusabile** per Connections. La serializzazione dei messaggi del protocollo applicativo (comandi client→server, notifiche server→client) passerà quasi certamente da:
1. Serializzazione di un oggetto/comando in una stringa (probabilmente JSON via GSON, secondo quanto già discusso per l'Assignment 10),
2. Conversione in byte con `getBytes(StandardCharsets.UTF_8)`,
3. Scrittura nel buffer, `flip()`, invio tramite `SocketChannel.write(buffer)`.

**Attenzione**: questo esempio usa `ByteBuffer.allocate()` (non-direct) e assume che l'intero messaggio arrivi con una sola `put`/`get` — semplificazione valida per un esempio didattico, ma nel progetto reale, dato che i `SocketChannel` NIO possono restituire letture parziali, sarà necessario gestire il caso in cui `read()` non riempia il buffer con il messaggio completo (framing del protocollo).

### Copia di file con Channel-to-Channel (pattern read/flip/write/compact)

```java
private static void channelCopy1(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (src.read(buffer) != -1) {
        buffer.flip();
        dest.write(buffer);
        buffer.compact();
    }
    buffer.flip();
    while (buffer.hasRemaining()) {
        dest.write(buffer);
    }
}
```

**Rilevanza per il progetto**: questo è **il pattern canonico da adattare per la comunicazione via `SocketChannel`** invece che `FileChannel`. La struttura `read → flip → write → compact` (o `clear` se tutto è stato scritto) è esattamente il ciclo che andrà implementato nel client NIO per inviare/ricevere comandi al server. Da notare l'uso di `hasRemaining()` per gestire scritture parziali — comportamento che sui socket (specie non bloccanti) è la norma, non l'eccezione.

### Variante con `while(hasRemaining())` interno al ciclo di lettura

```java
private static void channelCopy2(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (src.read(buffer) != -1) {
        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
        buffer.clear();
    }
}
```

**Rilevanza**: mostra la gestione robusta del caso in cui `write()` non scarichi tutti i byte in un'unica chiamata (comune sui socket). Questo ciclo interno `while (hasRemaining())` andrà probabilmente replicato nell'invio dei messaggi del protocollo Connections.

### Creazione di FileChannel da stream (classi "ponte")

```java
FileInputStream fin = new FileInputStream("example.txt");
FileChannel fc = fin.getChannel();
```

**Rilevanza per il progetto**: utile principalmente per la persistenza dei dati JSON (se si opta per un approccio a stream classico combinato con canali), meno direttamente applicabile ai socket, ma utile per capire il meccanismo di "bridging" tra le due API.

---

## 3. Integrazione di Codice Esterno

I seguenti elementi della Standard Library **non** compaiono nel file caricato ma sono necessari o fortemente consigliati per completare i requisiti del progetto Connections relativi alla comunicazione NIO lato client.

### `java.nio.channels.SocketChannel`

Non presente nella lezione (rimandato a lezioni successive), ma è **la classe cardine** per l'implementazione della connessione TCP persistente lato client richiesta dalle specifiche. Va aperta in modalità non bloccante per sfruttare appieno NIO.

```java
SocketChannel channel = SocketChannel.open();
channel.connect(new InetSocketAddress("localhost", 12345));
channel.configureBlocking(false); // necessario per NIO non bloccante
```

### `java.nio.charset.Charset` (oltre a `StandardCharsets`)

Il file usa già `StandardCharsets.UTF_8`, ma per un controllo più fine sulla codifica (ad esempio con `CharsetEncoder`/`CharsetDecoder` per conversioni dirette tra `CharBuffer` e `ByteBuffer`) è utile conoscere l'API completa.

```java
Charset charset = StandardCharsets.UTF_8;
CharBuffer charBuffer = CharBuffer.wrap("comando:JOIN_GAME");
ByteBuffer byteBuffer = charset.encode(charBuffer);
```

### `java.io.IOException` e gestione degli errori di rete

Nella comunicazione client-server è necessario distinguere e gestire correttamente le eccezioni derivanti da chiusura della connessione o errori di rete (ad esempio `ClosedChannelException`, sottoclasse di `IOException`), non trattate nel file.

```java
try {
    int bytesRead = channel.read(buffer);
    if (bytesRead == -1) {
        // il peer ha chiuso la connessione
        channel.close();
    }
} catch (IOException e) {
    // gestione disconnessione anomala
}
```

### `java.util.concurrent.BlockingQueue` (es. `LinkedBlockingQueue`)

Non trattata nella lezione sui Buffer/Channel, ma essenziale per disaccoppiare il thread NIO del client (che legge dal `SocketChannel`) dal thread che gestisce l'interfaccia utente o la logica applicativa, evitando di bloccare il ciclo di I/O.

```java
BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
// il thread NIO produce:
incomingMessages.put(decodedMessage);
// un thread separato consuma:
String msg = incomingMessages.take();
```

### `java.nio.ByteBuffer.wrap(byte[])`

Metodo statico alternativo ad `allocate()`, utile per creare rapidamente un `ByteBuffer` a partire da un array di byte già esistente (ad esempio l'output di una serializzazione GSON), evitando una copia esplicita con `put()`.

```java
byte[] jsonBytes = gson.toJson(comando).getBytes(StandardCharsets.UTF_8);
ByteBuffer buffer = ByteBuffer.wrap(jsonBytes); // già in draining mode, non serve flip()
```


---
# Lezione 10: Serializzazione, JSON, la libreria GSON

*(Reti e Laboratorio, Modulo Laboratorio III — docente D. Di Francesco Maesa, 24/11/2025)*

---

## 1. Teoria

### 1.1 Serializzazione: concetti di base

Un oggetto Java esiste solo finché è in esecuzione la JVM. Per renderlo persistente o per trasmetterlo su rete occorre una rappresentazione **indipendente dalla JVM**:

- **Serializzazione**: "flattening" dello stato dell'oggetto (i valori dei campi) in una sequenza di byte/testo.
- **Deserializzazione**: operazione inversa, ricostruisce lo stato dell'oggetto a partire dalla rappresentazione.
- L'oggetto serializzato può essere scritto su un qualsiasi **stream di output** (file, memoria, rete).

**Collegamento diretto al progetto**: nel documento di specifica di [[laboratorio-3]] la serializzazione va usata per due scopi precisi:
1. Inviare oggetti su uno **stream TCP** (connessione persistente client-server).
2. Generare **pacchetti UDP** per le notifiche asincrone (si serializza l'oggetto su uno stream di byte, poi si costruisce il pacchetto).

### 1.2 Formati di serializzazione: JSON vs XML

Per l'interoperabilità tra linguaggi/macchine diverse, i formati principali sono XML e **JSON**. JSON è preferito perché più leggero e compatto (XML è più verboso e pesante). Struttura di base di JSON:

- **oggetto**: insieme non ordinato di coppie `chiave: valore`, delimitato da `{ }`, chiavi sempre stringhe.
- **array**: raccolta ordinata di valori, delimitato da `[ ]`.
- Tipi di valore ammessi: `String`, `Number`, `object` (ricorsivo), `Array`, `Boolean`, `null`.
- La composizione ricorsiva di oggetti e array genera una **struttura ad albero** — è esattamente il formato richiesto dal protocollo applicativo del progetto (vedi Allegato 1 delle specifiche: ogni operazione è un JSON object con campo `operation` e altri campi specifici).

### 1.3 GSON: la libreria di riferimento del corso

Tra le librerie Java per la conversione Java↔JSON (GSON, JSON-Simple, Jackson, FastJSON, ecc.), il corso adotta **Google GSON**, libreria open-source che effettua la traduzione senza configurazioni complicate e gestisce liste, mappe, generics e oggetti annidati.

GSON offre **tre API distinte**, ciascuna con un caso d'uso ideale — punto cruciale per le scelte implementative del progetto:

| API | Quando usarla |
|---|---|
| **Data Binding** | JSON con struttura stabile → oggetti POJO Java. Uso reflection. |
| **Tree Model** | JSON variabile/dinamico → struttura ad albero navigabile (`JsonObject`, `JsonArray`). Non serve conoscere a priori la struttura. |
| **Streaming** | JSON di grandi dimensioni o ricevuto in streaming (es. su una connessione di rete) → elaborazione token per token, senza caricare tutto in memoria. Più veloce ma richiede più codice manuale. |

**Perché è rilevante per il progetto**: il protocollo Connections scambia messaggi JSON di piccola dimensione e struttura fissa (i comandi dell'Allegato 1) → **Data Binding** è la scelta naturale per (de)serializzare le richieste/risposte client-server. La **Streaming API**, invece, è esplicitamente richiesta nell'Assignment 10 per leggere file JSON di grandi dimensioni (analogia diretta con la lettura del file JSON delle parole/gruppi di gioco lato server, se di dimensioni considerevoli, o più in generale per casi in cui non si vuole caricare l'intero documento in RAM).

### 1.4 Data Binding: dettagli operativi

- Creazione dell'istanza: `new Gson()` (configurazione di default) oppure `new GsonBuilder().create()` (permette override: pretty printing, gestione `Date`, esclusione campi, version control).
- Serializzazione: `gson.toJson(oggetto)` → `String`.
- Deserializzazione: `gson.fromJson(jsonString, ClasseTarget.class)` → oggetto Java, tramite **reflection**.
- Il campo marcato `transient` **non viene serializzato** — utile per dati sensibili (es. potrebbe servire per non serializzare la password in chiaro lato server, se mai tenuta in un oggetto User insieme ad altri campi da inviare al client).
- Non si possono serializzare oggetti con **riferimenti circolari** (ricorsione infinita).
- Per **collezioni generiche** (es. `List<User>`), la cancellazione dei tipi generici a runtime richiede l'uso di `TypeToken` per preservare l'informazione di tipo durante la deserializzazione.

### 1.5 Tree Model e Streaming: dettagli operativi

- **Tree Model**: `JsonParser.parseString(...)` o `JsonParser.parseReader(...)` restituiscono un `JsonElement` (nodo radice), navigabile con `getAsJsonObject()`, `getAsJsonArray()`, `get("campo")`, ecc. Classi principali: `JsonElement`, `JsonObject`, `JsonArray`, `JsonPrimitive`, `JsonNull`.
- **Streaming**: `JsonReader` (lettura) e `JsonWriter` (scrittura) elaborano il documento **token per token** (`BEGIN_OBJECT`, `NAME`, `NEXTSTRING`, `END_ARRAY`, ecc.), senza mai avere l'intero JSON in memoria.

### 1.6 Collegamento diretto con l'Assignment 10

L'assignment allegato alla lezione è particolarmente rilevante perché è **strutturalmente identico** al problema del server di Connections:

- File JSON di grandi dimensioni (compresso GZIP) → letto con **GSON Streaming API**.
- Un thread produttore legge gli oggetti dal file e li distribuisce a un **thread pool**.
- I thread consumatori aggiornano una **struttura dati condivisa e sincronizzata** (conteggio movimenti per causale).

Questo è essenzialmente lo stesso pattern architetturale richiesto per il server di Connections: un componente che legge/riceve dati, li distribuisce a un pool di thread, i quali operano concorrentemente su strutture dati condivise sincronizzate (utenti registrati, stato della partita corrente, punteggi).

---

## 2. Codice Utile dal File

### 2.1 Data Binding — serializzazione/deserializzazione base

Direttamente applicabile alla codifica/decodifica dei messaggi JSON del protocollo (Allegato 1: `register`, `login`, `submitProposal`, ecc.).

```java
import com.google.gson.*;

public class ToGSON {
    public static void main(String[] args) {
        Person p = new Person("Alice", 59, "XC5F");
        Gson gson = new Gson();
        String json = gson.toJson(p);
        System.out.println(json);
    }
}
```

**Rilevanza per il progetto**: questo è esattamente il meccanismo da usare per trasformare, ad esempio, un oggetto `SubmitProposalRequest` (con campi `operation` e `words`) nel JSON da inviare sulla connessione TCP, e viceversa per interpretare i messaggi ricevuti dal server.

### 2.2 Data Binding con `GsonBuilder` e pretty printing

```java
Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
String json = gson.toJson(p);
```

**Rilevanza**: utile in fase di debug/logging del protocollo applicativo, per ispezionare i messaggi JSON scambiati durante lo sviluppo.

### 2.3 Deserializzazione con Data Binding

```java
import com.google.gson.Gson;

public class GsonFromJson {
    public static void main(String[] args) {
        String json_string = "{\"firstName\":\"Laura\", \"lastName\": \"Ricci\"}";
        Gson gson = new Gson();
        User user = gson.fromJson(json_string, User.class);
        System.out.println(user);
    }
}
```

**Rilevanza**: modello diretto per il server (o il client) che riceve una stringa JSON dal socket e deve ricostruire l'oggetto richiesta Java corrispondente in base al valore del campo `operation`.

### 2.4 Deserializzazione di collezioni generiche con `TypeToken`

```java
Type listType = new TypeToken<List<User>>(){}.getType();
List<User> users = gson.fromJson(jsonString, listType);
```

**Rilevanza**: necessario per operazioni come `requestLeaderboard` (che restituisce una lista di utenti/punteggi) o `requestGameStats` (lista di giocatori connessi con le loro performance) — qualunque risposta JSON che rappresenti una collezione di oggetti Java.

### 2.5 Tree Model API — lettura del file JSON delle partite

```java
File input = new File("restaurant.json");
JsonElement fileElement = JsonParser.parseReader(new FileReader(input));
JsonObject fileObject = fileElement.getAsJsonObject();
String identifier = fileObject.get("name").getAsString();
JsonArray jsonArrayOfItems = fileObject.get("menu").getAsJsonArray();
```

**Rilevanza diretta**: questo è il pattern da adattare per leggere il **file JSON delle partite** menzionato nelle specifiche del progetto ("il server ha a disposizione un file contenente le informazioni per ogni partita... in formato JSON"), utile in particolare se la struttura non è nota a priori o si preferisce non definire subito le classi POJO corrispondenti.

### 2.6 GSON Streaming API — `JsonReader` / `JsonWriter`

```java
JsonReader reader = new JsonReader(new FileReader("result.json"));
reader.beginObject();
while (reader.hasNext()) {
    String name = reader.nextName();
    if ("name".equals(name)) {
        System.out.println(reader.nextString());
    } else {
        reader.skipValue();
    }
}
reader.endObject();
reader.close();
```

**Rilevanza diretta e forte**: è il meccanismo esplicitamente richiesto dall'Assignment 10 (identico architetturalmente al problema del server Connections) per leggere in modo efficiente un file JSON di grandi dimensioni senza caricarlo interamente in memoria — applicabile alla lettura del file delle partite se contiene molte parole/gruppi, o più in generale ogni volta che il server deve processare dati JSON di dimensione non trascurabile in modo incrementale.

---

## 3. Integrazione di Codice Esterno

Elementi della Java Standard Library **non presenti nel file** ma necessari per completare i requisiti tecnici del progetto legati a questa lezione (comunicazione TCP/UDP su cui viaggiano i messaggi JSON serializzati con GSON).

### 3.1 `java.io.BufferedReader` / `java.io.PrintWriter` su socket TCP

Per leggere/scrivere le stringhe JSON su una connessione TCP riga per riga (utile come alternativa più semplice a NIO puro nelle fasi iniziali di prototipazione, anche se il progetto richiede NIO per l'implementazione finale lato client).

```java
BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
out.println(jsonRequest);       // invio del JSON serializzato
String jsonResponse = in.readLine();  // ricezione della risposta
```

### 3.2 `java.nio.channels.SocketChannel` — framing dei messaggi JSON su NIO

Poiché il progetto richiede esplicitamente NIO lato client per la connessione TCP persistente, e TCP è uno stream di byte senza confini di messaggio, serve gestire il **framing**: un modo comune è leggere in un `ByteBuffer` e delimitare i messaggi JSON con un carattere di terminazione (es. `\n`) o con un prefisso di lunghezza.

```java
ByteBuffer buffer = ByteBuffer.allocate(1024);
int bytesRead = channel.read(buffer);
buffer.flip();
String jsonChunk = StandardCharsets.UTF_8.decode(buffer).toString();
```

### 3.3 `java.net.DatagramSocket` / `DatagramPacket` — invio del JSON serializzato via UDP

Le specifiche indicano che, per le notifiche asincrone UDP, si scrive l'oggetto serializzato su uno stream di byte e si genera un pacchetto UDP.

```java
String jsonNotification = gson.toJson(notification);
byte[] data = jsonNotification.getBytes(StandardCharsets.UTF_8);
DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientUdpPort);
udpSocket.send(packet);
```

### 3.4 `java.util.concurrent.ExecutorService` — thread pool per il produttore/consumatore

Pattern esplicitamente richiesto dall'Assignment 10 (lettura file → distribuzione a thread pool) e dal server di Connections (gestione multithreaded con thread pooling).

```java
ExecutorService pool = Executors.newFixedThreadPool(8);
pool.submit(() -> processContoCorrente(contoCorrente));
```

### 3.5 `java.util.concurrent.ConcurrentHashMap` — struttura dati condivisa sincronizzata

Per accumulare in modo thread-safe i risultati calcolati dai thread del pool (nell'assignment: conteggio movimenti per causale; nel progetto: punteggi/stato dei giocatori).

```java
ConcurrentHashMap<String, AtomicInteger> countByType = new ConcurrentHashMap<>();
countByType.computeIfAbsent(causale, k -> new AtomicInteger(0)).incrementAndGet();
```

---

*Nota: questa lezione fornisce gli strumenti per la (de)serializzazione JSON del protocollo applicativo. Le prossime lezioni su NIO, socket e thread pooling completeranno il quadro per l'architettura client-server vera e propria.*