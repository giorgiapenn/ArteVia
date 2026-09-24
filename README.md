# ArteVia - Secure Art Marketplace

ArteVia è un'applicazione web per la gestione di un piccolo **marketplace dedicato all'arte e ai materiali artistici**, sviluppata con Spring Boot.

L'applicazione permette agli utenti di creare un account, gestire un wallet virtuale, sottoscrivere un abbonamento Insider, acquistare prodotti dal catalogo e visualizzare opere provenienti dalla collezione dell'**Art Institute of Chicago**.

La sicurezza costituisce una parte centrale del progetto: autenticazione stateless tramite JWT, refresh token persistiti e ruotati, autorizzazione basata sui ruoli, validazione degli input, protezione delle operazioni finanziarie e gestione delle richieste concorrenti sono implementate direttamente nei diversi livelli dell'applicazione.

L'applicazione è accessibile tramite **HTTPS** e mette a disposizione sia un'interfaccia web basata su Thymeleaf sia una REST API utilizzabile con client esterni.

---

## Indice

1. [Panoramica](#1-panoramica)
2. [Funzionalità](#2-funzionalità)
3. [Tecnologie](#3-tecnologie)
4. [Architettura](#4-architettura)
5. [Modello dati](#5-modello-dati)
6. [Screenshot dell'applicazione](#6-screenshot-dellapplicazione)
7. [Security Design](#7-security-design)
8. [Flussi principali](#8-flussi-principali)
9. [Avvio del progetto](#9-avvio-del-progetto)
10. [Utilizzo](#10-utilizzo)
11. [Configurazione](#11-configurazione)
12. [Gestione degli utenti amministratori](#12-gestione-degli-utenti-amministratori)
13. [Struttura del progetto](#13-struttura-del-progetto)
14. [REST API](#14-rest-api)
15. [Verifica e test](#15-verifica-e-test)
16. [Stato del progetto](#16-stato-del-progetto)
17. [Glossario dei concetti di sicurezza](#17-glossario-dei-concetti-di-sicurezza)

---

## 1. Panoramica

ArteVia utilizza un'architettura monolitica organizzata secondo una separazione tra:

* livello web e REST;
* servizi applicativi;
* persistenza;
* componenti dedicati alla sicurezza;
* integrazione con servizi esterni.

Il browser utilizza le pagine Thymeleaf e le chiamate JavaScript verso la REST API. Gli stessi endpoint possono essere utilizzati indipendentemente tramite strumenti come Postman o `curl`.

Dal punto di vista funzionale, il dominio è organizzato intorno a tre elementi principali:

1. **utenti e autenticazione**;
2. **wallet e pagamenti interni**;
3. **catalogo e acquisti**.

Le operazioni che modificano contemporaneamente più risorse, come il checkout, vengono eseguite all'interno di transazioni database e utilizzano optimistic locking per gestire richieste concorrenti.

---

## 2. Funzionalità

### 2.1 Account e accesso

La registrazione crea un normale account `USER` e associa automaticamente un wallet.

L'accesso può essere effettuato utilizzando username oppure email: un identificativo che contiene `@` viene cercato solo come email (gli username non possono contenere `@`), altrimenti solo come username. Dopo l'autenticazione vengono gestiti:

* access token JWT;
* refresh token persistito;
* cookie `HttpOnly`;
* cookie `Secure`;
* `SameSite=Lax`;
* rinnovo automatico dell'access token;
* invalidazione e rotazione dei refresh token.

Il ruolo dell'utente non viene inserito nel JWT: viene recuperato dal database quando viene costruito il contesto di sicurezza.

### 2.2 Wallet

Ogni account dispone di un wallet virtuale utilizzabile per le operazioni del marketplace.

Sono disponibili:

* visualizzazione del saldo;
* ricarica;
* controllo del saldo prima degli acquisti;
* aggiornamento concorrente protetto tramite `@Version`.

Il wallet non rappresenta un sistema di pagamento reale: viene utilizzato esclusivamente come meccanismo interno al progetto.

### 2.3 Catalogo

Il catalogo comprende diverse tipologie di prodotti, tra cui:

* stampe;
* dipinti;
* materiali per artisti;
* strumenti per il disegno e la pittura.

Ogni prodotto dispone di prezzo, descrizione, categoria e quantità disponibile.

Gli utenti possono consultare il catalogo e aggiungere gli articoli al carrello.

### 2.4 Acquisti

Il checkout non utilizza i prezzi ricevuti dal browser come fonte attendibile.

Per ogni articolo il server:

1. recupera il prodotto dal database;
2. controlla la disponibilità;
3. recupera il prezzo corrente;
4. calcola il totale, applicando l'eventuale sconto Insider attivo;
5. verifica il saldo;
6. aggiorna stock e wallet;
7. registra l'acquisto.

L'intera operazione viene eseguita in una singola transazione.

### 2.5 Insider Club

Gli utenti possono sottoscrivere un abbonamento Insider (piani con durata e sconto configurabili) che applica uno sconto percentuale automatico su ogni acquisto nel negozio. L'abbonamento attivo viene verificato lato server ad ogni checkout: il totale non è mai calcolato o modificato dal client, ma ricalcolato interamente dal backend leggendo lo stato reale dell'abbonamento dell'utente sul database, per evitare manomissioni dello sconto.

### 2.6 Opera in evidenza

L'applicazione integra l'API pubblica dell'**Art Institute of Chicago**.

La funzionalità `/api/v1/artwork/featured` recupera un'opera dalla collezione (filtrata lato ARTIC per garantire la presenza di un'immagine) e restituisce al frontend le informazioni necessarie per visualizzarla.

L'accesso al servizio esterno viene effettuato attraverso `WebClient` (con timeout di connessione e risposta configurati) e protetto da un rate limiter per singolo utente. L'immagine dell'opera non viene caricata direttamente dal browser verso il CDN esterno, ma scaricata dal server e inoltrata al client tramite un endpoint proxy dedicato (`/api/v1/artwork/image/{imageId}`): evita così che il browser blocchi la richiesta cross-origin (Opaque Response Blocking) e che eventuali protezioni anti-hotlink del CDN esterno impediscano la visualizzazione.

### 2.7 Funzionalità amministrative

Gli utenti con ruolo `ADMIN` possono aggiungere nuovi prodotti al catalogo tramite endpoint dedicati.

Il ruolo non può essere specificato durante la registrazione e gli endpoint amministrativi utilizzano `@PreAuthorize` per verificare l'autorizzazione.

---

## 3. Tecnologie

| Area                 | Tecnologia                   | Versione               |
| -------------- | ------------------- | ---------------- |
| Linguaggio           | Java                         | 21 LTS                 |
| Framework            | Spring Boot                  | 4.1.0                  |
| Security             | Spring Security + JJWT       | JJWT 0.12.6            |
| ORM                  | Spring Data JPA + Hibernate  | gestito da Spring Boot |
| Database applicativo | PostgreSQL                   | 17+                    |
| Database di test     | H2                           | gestito da Spring Boot |
| Frontend server-side | Thymeleaf                    | gestito da Spring Boot |
| Mapping              | MapStruct                    | 1.6.3                  |
| Boilerplate          | Lombok                       | gestito da Spring Boot |
| Resilienza           | Resilience4j                 | 2.2.0                  |
| Test coverage        | JaCoCo                       | 0.8.14                 |
| Build                | Maven                        | 3.9.16                 |
| API esterna          | Art Institute of Chicago API | v1                     |
| Protocollo           | HTTPS / TLS                  | PKCS12                 |

---

## 4. Architettura

L'elaborazione di una richiesta segue una catena simile a quella mostrata di seguito:

```mermaid
flowchart TB
    subgraph Client["Browser / Client REST"]
        B["Thymeleaf UI<br/>(login, profilo, negozio)"]
        R["Client REST<br/>(Postman/curl)"]
    end

    subgraph Security["Livello di sicurezza"]
        F["JwtFilter<br/>estrae/valida il JWT da header Authorization o cookie,<br/>gestisce il refresh silenzioso"]
        WSC["WebSecurityConfig<br/>CORS, CSRF disabilitato (SameSite=Lax + JSON),<br/>regole di autorizzazione"]
    end

    subgraph Controllers["Controller"]
        AC["AuthController<br/>/api/v1/auth/**"]
        AP["ApiController<br/>/api/v1/**"]
        AD["AdminController<br/>/api/v1/admin/** (ROLE_ADMIN)"]
        PC["PageController<br/>rendering Thymeleaf"]
    end

    subgraph Services["Livello di servizio"]
        AS["AuthService"]
        RTS["RefreshTokenService"]
        WS["WalletService"]
        PS["ProductService"]
        SS["ShopService"]
        MS["MembershipService"]
        AWS["ArtworkOfDayService"]
    end

    subgraph Data["Persistenza"]
        REPO["Repository JPA"]
        PG[("PostgreSQL")]
    end

    EXT["Art Institute of Chicago API<br/>(rate limited per utente)"]

    B -->|HTTPS| F
    R -->|HTTPS + Bearer| F
    F --> WSC
    F -.->|refresh silenzioso| RTS
    WSC --> AC & AP & AD & PC
    AC --> AS
    AP --> WS & SS & MS & AWS
    AD --> PS
    AS & RTS & WS & PS & SS & MS --> REPO
    REPO --> PG
    AWS -->|WebClient + Resilience4j| EXT
```
`AdminController` espone solo l'inserimento dei prodotti e usa `ProductService`; un utente con ruolo `ADMIN` può usare anche tutti gli endpoint di `ApiController`, che richiedono soltanto l'autenticazione.

### 4.1 Livelli principali

**Security layer**

Gestisce autenticazione, estrazione dei token, costruzione del `SecurityContext`, CORS e autorizzazione.

Componenti principali:

* `JwtFilter`;
* `JwtService`;
* `CookieUtils`;
* `UserDetailsServiceImpl`;
* `WebSecurityConfig`.

**Controller layer**

Espone le funzionalità dell'applicazione attraverso endpoint REST e pagine Thymeleaf.

Sono presenti controller separati per:

* autenticazione;
* funzionalità utente;
* amministrazione;
* rendering delle pagine.

**Service layer**

Contiene la logica applicativa. In particolare:

* `AuthService`;
* `RefreshTokenService`;
* `WalletService`;
* `ProductService`;
* `ShopService`;
* `MembershipService`;
* `ArtworkOfDayService`.

**Persistence layer**

La persistenza è realizzata attraverso Spring Data JPA e PostgreSQL.

Le entity non vengono utilizzate direttamente come contratto della REST API: le richieste e le risposte passano attraverso DTO dedicati.

---

## 5. Modello dati

Le principali relazioni tra le entità sono:

```mermaid
erDiagram
    USER ||--|| WALLET : owns
    USER ||--o{ REFRESH_TOKEN : uses
    USER ||--o{ PURCHASED_PRODUCT : creates
    PRODUCT ||--o{ PURCHASED_PRODUCT : references
    USER ||--o{ CLUB_MEMBERSHIP : sottoscrive
    CLUB_PLAN ||--o{ CLUB_MEMBERSHIP : definisce

    CLUB_PLAN {
        Long id PK
        String name
        BigDecimal price
        Integer durationDays
        Integer discountPercentage
    }
    CLUB_MEMBERSHIP {
        Long id PK
        Long userId FK
        Long planId FK
        LocalDateTime startDate
        LocalDateTime endDate
        boolean active
    }
```

### 5.1 Entità principali

| Entità             | Responsabilità                                          |
| ------------- | --------------------------------------- |
| `User`             | Dati dell'account e ruolo                                 |
| `Wallet`           | Saldo virtuale dell'utente                                 |
| `RefreshToken`     | Sessione di refresh persistita                             |
| `Product`          | Articolo presente nel catalogo                             |
| `PurchasedProduct` | Registrazione di un acquisto                               |
| `ClubPlan`         | Piano di abbonamento Insider (nome, prezzo, sconto, durata) |
| `ClubMembership`   | Abbonamento attivo di un utente a un piano, con validità    |
| `Role`             | Ruoli `USER` e `ADMIN`                                     |

---

## 6. Screenshot dell'applicazione

### 6.1 Home
![Home](screenshots/home.png)

### 6.2 Registrazione
![Registrazione](screenshots/registrazione_1.png)

### 6.3 Validazione campi in fase di registrazione
![Registrazione dettaglio](screenshots/registrazione_2.png)

### 6.4 Login
![Login](screenshots/login.png)

### 6.5 Shop
![Shop](screenshots/shop.png)

### 6.6 Carrello
![Carrello](screenshots/carrello.png)

### 6.7 Abbonamenti
![Abbonamenti](screenshots/abbonamenti.png)

### 6.8 Storico Acquisti
![Storico](screenshots/storico.png)

### 6.9 Opera del giorno
![Opera del giorno](screenshots/opera.png)

---

## 7. Security Design

La sicurezza non è concentrata in un unico componente, ma viene applicata in diversi punti del flusso di una richiesta.

### 7.1 Autenticazione

ArteVia utilizza due token con responsabilità differenti.

#### Access token

L'access token è un JWT:

* durata: 15 minuti;
* firma HMAC;
* `HttpOnly`;
* `Secure`;
* `SameSite=Lax`;
* subject contenente lo username.

Il JWT non contiene il ruolo dell'utente. Questo permette al sistema di non utilizzare un ruolo eventualmente diventato obsoleto nel token: il ruolo viene invece recuperato dal database quando viene costruito il contesto di autenticazione.

Per i client REST è inoltre possibile inviare il token attraverso:

```http
Authorization: Bearer <token>
```

Se la richiesta contiene sia l'header `Authorization` sia il cookie `accessToken`, ha la precedenza l'header.

#### Refresh token

Il refresh token è diverso dall'access token:

* non è un JWT;
* è una stringa casuale;
* viene memorizzato nel database;
* ha durata di 30 giorni;
* viene ruotato quando viene utilizzato;
* viene inviato al browser attraverso un cookie protetto.

Quando un refresh token viene utilizzato con successo, il valore precedente viene invalidato e sostituito.

#### Refresh automatico

Il `JwtFilter` controlla le richieste in ingresso. Quando l'access token non è più valido perché scaduto, il filtro può utilizzare il refresh token ancora valido per generare una nuova coppia di credenziali. Il meccanismo è trasparente al browser e non richiede un nuovo login.

Se più richieste parallele arrivano con lo stesso refresh token, solo la prima esegue la rotazione: le altre ricevono `401`, senza generare errori `500`.

### 7.2 Controllo degli accessi

Le funzionalità applicative non sono tutte disponibili allo stesso livello.

```text
PUBLIC (nessuna autenticazione)
 |-- registrazione, login, refresh, logout    /api/v1/auth/**
 |-- pagine home, login e registrazione       /, /auth/**
 |-- risorse statiche                         /css/**, /js/**, /images/**
 |-- stato dell'applicazione                  /actuator/health

USER (qualsiasi utente autenticato, compreso ADMIN)
 |-- wallet e ricarica                        /api/v1/wallet/**
 |-- catalogo, carrello e checkout            /api/v1/products, /api/v1/shop/**
 |-- storico acquisti                         /api/v1/user/shop/history
 |-- piani e abbonamento Insider              /api/v1/plans, /api/v1/membership/**
 |-- opera in evidenza e immagine             /api/v1/artwork/**
 |-- pagine profilo e negozio                 /home/**

ADMIN (solo ruolo ADMIN)
 |-- inserimento prodotti nel catalogo        POST /api/v1/admin/products
```

Una richiesta non autenticata verso `/api/**` riceve `401 Unauthorized` (JSON); una richiesta non autenticata verso una pagina viene reindirizzata a `/auth/login`. Un utente autenticato che non ha il ruolo richiesto riceve `403 Forbidden`.

Gli endpoint amministrativi utilizzano:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Il ruolo non viene accettato come parametro durante la registrazione. Un nuovo account viene sempre creato come `USER`.

### 7.3 Protezione delle operazioni di checkout

Il checkout rappresenta una delle operazioni più delicate perché modifica contemporaneamente più risorse.

La procedura viene eseguita in una transazione:

```java
@Transactional
```

Il server non considera attendibili né il prezzo né lo stock ricevuti dal client. Inoltre, `Product` e `Wallet` utilizzano optimistic locking attraverso `@Version`. In caso di aggiornamento concorrente, una delle operazioni può fallire con un conflitto invece di produrre uno stato incoerente.

**Proprietà che il checkout deve preservare:**

```text
per ogni prodotto:
quantità richiesta  <=  stock disponibile (letto dal DB)

prezzo unitario pagato  =  prezzo del DB × (100 − sconto dell'abbonamento attivo) / 100
                           arrotondato a 2 decimali
totale                  =  Σ (prezzo unitario pagato × quantità)

totale  <=  saldo disponibile
```

Solo dopo il superamento delle verifiche vengono aggiornati: stock, saldo, storico degli acquisti.

### 7.4 Validazione degli input

Le richieste HTTP vengono rappresentate tramite DTO specifici. Esempi: `RegisterRequest`, `LoginRequest`, `RechargeRequest`, `ProductCreateRequest`, `CheckoutRequest`, `BuyMembershipRequest`.

Le annotazioni Jakarta Validation vengono utilizzate per controllare, tra le altre cose: campi obbligatori, formato email, requisiti della password, quantità, importi, valori non negativi.

Le entity JPA non costituiscono quindi direttamente il contratto di input dell'API.

### 7.5 Controlli di sicurezza implementati

| Scenario                                 | Meccanismo utilizzato                          |
| ---------------------------- | --------------------------------- |
| Furto dell'access token                  | breve durata + cookie `HttpOnly` e `Secure`    |
| Riutilizzo di un refresh token           | rotazione e invalidazione del token precedente |
| Modifica del ruolo tramite registrazione | DTO senza proprietà `role`                     |
| Accesso alle funzioni admin              | `@PreAuthorize` + ruolo verificato dal DB      |
| Alterazione del prezzo nel checkout      | prezzo recuperato dal database                 |
| Alterazione dello sconto Insider         | stato dell'abbonamento verificato dal database |
| Acquisto oltre lo stock disponibile      | controllo server-side + optimistic locking     |
| Doppio aggiornamento del wallet          | `@Version` + transazione                       |
| Stato parzialmente aggiornato            | `@Transactional`                               |
| Abuso dell'API esterna                   | rate limiting per utente                       |
| Input non valido                         | Jakarta Validation                             |
| Inserimento di HTML/JS nel frontend      | escaping tramite `escapeHtml()`                |
| Overflow della quantità nel carrello     | `@Max` sulla quantità + `Math.addExact`        |
| XSS memorizzato (stored XSS)             | escaping in output, verificato con prodotto malevolo |

---

## 8. Flussi principali

### 8.1 Autenticazione

```mermaid
sequenceDiagram
    participant U as Client
    participant A as AuthController
    participant S as AuthService
    participant AM as AuthenticationManager
    participant DB as PostgreSQL

    U->>A: POST /api/v1/auth/login
    A->>S: login(usernameOrEmail, password)
    alt identificativo con @
        S->>DB: cerca l'utente per email
        DB-->>S: username (se l'email non esiste resta l'identificativo)
    else identificativo senza @
        S->>S: usa l'identificativo come username
    end
    S->>AM: authenticate(username, password)
    AM->>DB: carica l'utente per username
    DB-->>AM: hash BCrypt e ruolo
    AM->>AM: confronta la password con l'hash
    AM-->>S: autenticazione riuscita (altrimenti 401)
    S->>DB: recupera l'utente per username
    DB-->>S: utente
    S->>S: genera access token JWT (15 minuti)
    S->>DB: salva refresh token (UUID, 30 giorni)
    S-->>A: coppia di token
    A-->>U: 200 + Set-Cookie accessToken e refreshToken
```

### 8.2 Acquisto

```mermaid
sequenceDiagram
    participant U as Client
    participant API as ApiController
    participant S as ShopService
    participant DB as PostgreSQL

    U->>API: POST /api/v1/shop/checkout
    API->>S: checkout(utente, items)
    S->>S: unisce le righe dello stesso prodotto
    S->>DB: recupera wallet
    DB-->>S: wallet
    S->>DB: recupera prodotti
    DB-->>S: prodotti (prezzo e stock)
    S->>S: verifica esistenza e stock
    S->>DB: recupera abbonamento attivo
    DB-->>S: abbonamento
    S->>S: calcola il totale scontato e verifica il saldo
    S->>DB: aggiorna stock
    S->>DB: registra acquisto
    S->>DB: aggiorna saldo
    DB-->>S: commit
    S-->>API: risultato
    API-->>U: 200 con totale e nuovo saldo
```

---

## 9. Avvio del progetto

### Requisiti

* Java 21 o superiore;
* PostgreSQL 17 o superiore;
* Maven oppure il Maven Wrapper incluso nel repository;
* Docker, se si sceglie di utilizzare il database tramite container.

### 1. Clonazione

```bash
git clone https://github.com/giorgiapenn/ArteVia.git
cd ArteVia
```

### 2. Database

```bash
docker compose up -d
```

In alternativa, un'istanza PostgreSQL 17+ locale con un database `ARTEVIA` creato manualmente.

### 3. Certificato HTTPS

Il progetto include già un keystore PKCS12 con un certificato autofirmato in `src/main/resources/keystore.p12`: non è necessario generarlo per avviare l'applicazione.

#### Rigenerazione opzionale del keystore
Se si vuole creare un nuovo certificato autofirmato (es. dopo la scadenza dei 365 giorni), posizionarsi nella cartella delle risorse e rigenerare il file:

```bash
cd src/main/resources
keytool -genkeypair -alias https -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 365 -storepass changeit -dname "CN=localhost, OU=Dev, O=ArteVia, L=Roma, ST=Lazio, C=IT"
cd ../../..
```

### 4. Variabili d'ambiente (facoltativo)

La password del database e la chiave di firma dei JWT possono essere fornite tramite variabili d'ambiente. Il passo è facoltativo: se viene saltato, l'applicazione usa i valori di sviluppo descritti in [Credenziali e segreti](#111-credenziali-e-segreti).

Le variabili valgono solo per il terminale in cui vengono impostate, quindi l'avvio (passo 6) va eseguito nello stesso terminale. `DB_PASSWORD` deve coincidere con `POSTGRES_PASSWORD` di `docker-compose.yml`.

Linux / macOS:

```bash
export DB_PASSWORD=postgres
export JWT_SECRET='unaChiaveCasualeDiAlmeno32Caratteri'
```

Windows (PowerShell):

```powershell
$env:DB_PASSWORD="postgres"
$env:JWT_SECRET="unaChiaveCasualeDiAlmeno32Caratteri"
```

Il file `.env.example` elenca le variabili da impostare; Spring Boot non lo legge automaticamente.

### 5. Compilazione

```bash
./mvnw clean package -DskipTests
```

Su Windows (PowerShell): `.\mvnw.cmd clean package -DskipTests`
### 6. Avvio

```bash
./mvnw spring-boot:run
```

L'applicazione sarà disponibile su `https://localhost:8443`. Con `spring.jpa.hibernate.ddl-auto=update`, Hibernate crea le tabelle mancanti all'avvio.

### 7. Dati iniziali

```sql
INSERT INTO product (name, description, price, stock_quantity, category, version) VALUES
  ('Stampa Van Gogh - Notte Stellata', 'Stampa fine-art su carta cotone 300g', 39.90, 25, 'Stampe', 0),
  ('Tela dipinta a mano - Paesaggio astratto', 'Opera originale acrilico su tela 50x70', 249.00, 3, 'Dipinti', 0),
  ('Set 24 matite colorate professionali', 'Matite Prismacolor per illustrazione', 34.50, 40, 'Materiali', 0),
  ('Tavolozza acquerelli 36 colori', 'Colori acquerello per artisti', 28.00, 30, 'Materiali', 0),
  ('Cavalletto da tavolo in legno', 'Cavalletto pieghevole per tele fino a 40cm', 22.90, 18, 'Materiali', 0),
  ('Stampa Klimt - Il Bacio', 'Riproduzione museale certificata', 44.90, 20, 'Stampe', 0),
  ('Blocco carta pressata a caldo A3', '20 fogli per acquerello e china', 16.50, 50, 'Materiali', 0),
  ('Scultura in ceramica - Busto astratto', 'Pezzo unico fatto a mano', 189.00, 2, 'Dipinti', 0);

INSERT INTO club_plan (name, price, duration_days, discount_percentage) VALUES
  ('Insider Monthly', 9.90, 30, 5),
  ('Insider Annual', 89.00, 365, 15);
```

---

## 10. Utilizzo

### 10.1 Interfaccia web

1. aprire `https://localhost:8443`;
2. creare un account;
3. effettuare il login;
4. accedere alla pagina personale;
5. ricaricare il wallet;
6. sottoscrivere eventualmente un piano Insider;
7. visualizzare l'opera in evidenza;
8. aprire il catalogo;
9. aggiungere prodotti al carrello;
10. completare il checkout.

```text
/
|-- /auth/register
|-- /auth/login
|-- /home/profile
|-- /home/shop
```

### 10.2 Accesso REST

Tutte le funzioni sono disponibili anche come REST API, senza passare dal sito. Tutti gli endpoint hanno il prefisso `/api/v1` (elenco completo nella sezione [REST API](#14-rest-api)).

1. `POST /api/v1/auth/login` con username (o email) e password: la risposta contiene `accessToken` e `refreshToken`.
2. Ogni richiesta successiva a un endpoint protetto invia l'access token nell'header:

```http
Authorization: Bearer <access-token>
```

3. Dopo 15 minuti l'access token scade: si ottiene una nuova coppia con `POST /api/v1/auth/refresh` inviando il refresh token.

I client che gestiscono i cookie possono usare in alternativa `accessToken` e `refreshToken` impostati dal login.

---

## 11. Configurazione

`src/main/resources/application.properties`:

| Proprietà                                              | Funzione                                   | Valore                                     |
| ------------------------------------------------------ | ------------------------------------------ | ------------------------------------------ |
| `spring.datasource.url`                                | Connessione PostgreSQL                     | `jdbc:postgresql://localhost:5432/ARTEVIA` |
| `spring.datasource.username`                           | Utente database                            | `postgres`                                 |
| `spring.datasource.password`                           | Password database                          | `${DB_PASSWORD:postgres}`                  |
| `spring.jpa.hibernate.ddl-auto`                        | Gestione schema                            | `update`                                   |
| `jwt.secret`                                           | Chiave HMAC per la firma dei JWT           | `${JWT_SECRET:<chiave di sviluppo>}`       |
| `jwt.access-token.expiration-ms`                       | Durata access token                        | `900000` (15 minuti)                       |
| `jwt.refresh-token.expiration-ms`                      | Durata refresh token                       | `2592000000` (30 giorni)                   |
| `artic.api.base-url`                                   | Endpoint ARTIC                             | `https://api.artic.edu/api/v1`             |
| `resilience4j.ratelimiter.configs.articApi.*`          | Rate limit per utente                      | 5 richieste ogni 60 secondi                |
| `server.port`                                          | Porta HTTPS                                | `8443`                                     |
| `server.ssl.key-store`                                 | Keystore TLS                               | `classpath:keystore.p12`                   |
| `server.ssl.key-store-password`                        | Password keystore                          | `changeit`                                 |
| `server.ssl.key-alias`                                 | Alias certificato                          | `https`                                    |
| `management.endpoints.web.exposure.include`            | Endpoint Actuator esposti                  | `health,info`                              |

### 11.1 Credenziali e segreti

I valori presenti nel progetto sono destinati esclusivamente all'esecuzione locale.

* `spring.datasource.password` e `jwt.secret` usano la sintassi `${VARIABILE:default}`: se `DB_PASSWORD` o `JWT_SECRET` sono definite nell'ambiente (passo 4 dell'avvio) viene usato il loro valore, altrimenti quello dopo i due punti.
* I valori di default sono stati scelti di proposito: la password del database coincide con `POSTGRES_PASSWORD` di `docker-compose.yml` e la chiave JWT con quella di `.env.example`, così che l'applicazione si avvii anche senza configurare le variabili. In un ambiente reale i default andrebbero rimossi e i segreti forniti solo dall'ambiente.
* Il keystore incluso nel repository (password `changeit`) contiene un certificato di sviluppo self-signed e non deve essere utilizzato in produzione.

### 11.2 Database di test

`src/test/resources/application.properties` utilizza H2 in-memory: l'esecuzione della suite di test non modifica il database PostgreSQL utilizzato dall'applicazione.

### 11.3 CORS

Il backend configura il Cross-Origin Resource Sharing (CORS) per consentire, in ambiente di sviluppo, richieste provenienti da eventuali frontend separati eseguiti sulle seguenti origini:

* `http://localhost:5500`
* `http://127.0.0.1:5500`

Configurazione definita in `WebSecurityConfig.corsConfigurationSource()`.

Il frontend Thymeleaf integrato nell'applicazione utilizza invece la stessa origine del backend (`https://localhost:8443`) e quindi non richiede CORS per le proprie richieste: la configurazione serve solo per un eventuale client separato (es. un frontend statico servito su una porta diversa durante lo sviluppo).

---

## 12. Gestione degli utenti amministratori

La registrazione pubblica non permette di scegliere il ruolo dell'account. Il valore iniziale è sempre `USER`.

1. registrare normalmente l'utente;
2. accedere al database `ARTEVIA`;
3. modificare il ruolo:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE username = 'nome_utente';
```

Poiché il ruolo viene recuperato dal database durante l'autenticazione della richiesta, la modifica viene applicata senza dover inserire il ruolo nel JWT. Un account `USER` che prova a chiamare un endpoint amministrativo riceve `403 Forbidden`.

---

## 13. Struttura del progetto

```text
ArteVia/
|-- pom.xml
|-- lombok.config
|-- mvnw
|-- mvnw.cmd
|-- docker-compose.yml
|-- .env.example
|-- README.md
|-- screenshots/
|-- postman/
|   |-- ArteVia.postman_collection.json
|
|-- src/
|   |-- main/
|   |   |-- java/com/artevia/
|   |   |   |-- ArteViaApplication.java
|   |   |   |
|   |   |   |-- config/
|   |   |   |   |-- WebClientConfig.java
|   |   |   |
|   |   |   |-- controller/
|   |   |   |   |-- AdminController.java
|   |   |   |   |-- ApiController.java
|   |   |   |   |-- AuthController.java
|   |   |   |   |-- PageController.java
|   |   |   |
|   |   |   |-- dto/
|   |   |   |   |-- ArtworkDto.java
|   |   |   |   |-- BuyMembershipRequest.java
|   |   |   |   |-- CartItemRequest.java
|   |   |   |   |-- CheckoutRequest.java
|   |   |   |   |-- ClubPlanDto.java
|   |   |   |   |-- LoginRequest.java
|   |   |   |   |-- ProductCreateRequest.java
|   |   |   |   |-- ProductDto.java
|   |   |   |   |-- PurchasedProductDto.java
|   |   |   |   |-- RechargeRequest.java
|   |   |   |   |-- RegisterRequest.java
|   |   |   |   |-- TokenResponse.java
|   |   |   |   |-- WalletDto.java
|   |   |   |   |-- artic/
|   |   |   |       |-- ArticApiResponse.java
|   |   |   |
|   |   |   |-- exception/
|   |   |   |   |-- ExternalServiceUnavailableException.java
|   |   |   |   |-- GlobalExceptionHandler.java
|   |   |   |
|   |   |   |-- mapper/
|   |   |   |   |-- ClubPlanMapper.java
|   |   |   |   |-- ProductMapper.java
|   |   |   |   |-- PurchasedProductMapper.java
|   |   |   |   |-- WalletMapper.java
|   |   |   |
|   |   |   |-- model/
|   |   |   |   |-- ClubMembership.java
|   |   |   |   |-- ClubPlan.java
|   |   |   |   |-- Product.java
|   |   |   |   |-- PurchasedProduct.java
|   |   |   |   |-- RefreshToken.java
|   |   |   |   |-- Role.java
|   |   |   |   |-- User.java
|   |   |   |   |-- Wallet.java
|   |   |   |
|   |   |   |-- repository/
|   |   |   |   |-- ClubMembershipRepository.java
|   |   |   |   |-- ClubPlanRepository.java
|   |   |   |   |-- ProductRepository.java
|   |   |   |   |-- PurchasedProductRepository.java
|   |   |   |   |-- RefreshTokenRepository.java
|   |   |   |   |-- UserRepository.java
|   |   |   |   |-- WalletRepository.java
|   |   |   |
|   |   |   |-- security/
|   |   |   |   |-- CookieUtils.java
|   |   |   |   |-- JwtFilter.java
|   |   |   |   |-- JwtService.java
|   |   |   |   |-- UserDetailsImpl.java
|   |   |   |   |-- UserDetailsServiceImpl.java
|   |   |   |   |-- WebSecurityConfig.java
|   |   |   |
|   |   |   |-- service/
|   |   |       |-- ArtworkOfDayService.java
|   |   |       |-- AuthService.java
|   |   |       |-- MembershipService.java
|   |   |       |-- ProductService.java
|   |   |       |-- RefreshTokenService.java
|   |   |       |-- ShopService.java
|   |   |       |-- WalletService.java
|   |   |
|   |   |-- resources/
|   |       |-- templates/
|   |       |   |-- auth/
|   |       |   |   |-- login.html
|   |       |   |   |-- register.html
|   |       |   |-- home/
|   |       |   |   |-- profile.html
|   |       |   |   |-- shop.html
|   |       |   |-- index.html
|   |       |-- static/
|   |       |   |-- css/
|   |       |   |   |-- style.css
|   |       |   |-- images/
|   |       |-- application.properties
|   |       |-- application-dev.properties
|   |       |-- keystore.p12
|   |
|   |-- test/
|       |-- java/com/artevia/
|       |   |-- ArteViaApplicationIntegrationTests.java
|       |   |-- securitytest/
|       |   |   |-- AdminAccessControlIntegrationTest.java
|       |   |   |-- CheckoutTransactionIntegrationTest.java
|       |   |   |-- ConcurrentCheckoutIntegrationTest.java
|       |   |   |-- MassAssignmentIntegrationTest.java
|       |   |   |-- RateLimitingIntegrationTest.java
|       |   |   |-- RefreshTokenRotationIntegrationTest.java
|       |   |   |-- UserApiEndpointsIntegrationTest.java
|       |   |-- service/
|       |       |-- MembershipServiceTest.java
|       |       |-- ShopServiceTest.java
|       |       |-- WalletServiceTest.java
|       |
|       |-- resources/
|           |-- application.properties
```

---

## 14. REST API

Base path `/api/v1`. Le operazioni che richiedono autenticazione accettano il JWT tramite cookie oppure `Authorization: Bearer <token>`.

### 14.1 Account

#### `POST /api/v1/auth/register`
Crea un nuovo account. **Accesso:** pubblico
```json
{
  "username": "mario",
  "name": "Mario",
  "lastname": "Rossi",
  "email": "mario@test.com",
  "address": "Via Roma 1",
  "age": 25,
  "password": "Password1!"
}
```

#### `POST /api/v1/auth/login`
Effettua l'autenticazione. **Accesso:** pubblico
```json
{ "usernameOrEmail": "mario@test.com", "password": "Password1!" }
```
Risposta:
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 2592000
}
```

#### `POST /api/v1/auth/refresh`
Genera una nuova coppia di token utilizzando un refresh token valido; il precedente viene invalidato. Il refresh token viene letto dal cookie `refreshToken` oppure, in sua assenza, dal body:
```json
{ "refreshToken": "<refresh-token>" }
```
**Accesso:** pubblico

#### `POST /api/v1/auth/logout`
Invalida il refresh token (letto dal cookie `refreshToken` oppure dal body `{ "refreshToken": "..." }`) e cancella i cookie di autenticazione. **Accesso:** pubblico: l'operazione ha effetto solo se viene fornito un refresh token valido.

L'access token già emesso non viene revocato: essendo un JWT stateless resta valido fino alla sua scadenza naturale (al massimo 15 minuti). Per questo motivo la sua durata è volutamente breve.

### 14.2 Arte e contenuti

#### `GET /api/v1/artwork/featured`
Restituisce un'opera in evidenza proveniente dall'Art Institute of Chicago. **Accesso:** autenticato · **Rate limit:** 5 richieste/minuto per utente.
```json
{ "title": "...", "artist": "...", "date": "...", "imageUrl": "/api/v1/artwork/image/<image-id>" }
```
Se il limite viene superato: `429 Too Many Requests`

#### `GET /api/v1/artwork/image/{imageId}`
Scarica dal server l'immagine IIIF associata a un'opera e la inoltra al client come proxy, evitando che il browser effettui una richiesta diretta cross-origin verso il CDN di artic.edu. L'`imageId` viene validato con un'espressione regolare prima di costruire l'URL esterno. **Accesso:** autenticato · **Rate limit:** 5 richieste/minuto per utente, separato da quello di `/artwork/featured`.

Se il servizio esterno non è raggiungibile, entrambi gli endpoint rispondono `503 Service Unavailable`.

### 14.3 Insider Club

#### `GET /api/v1/plans`
Restituisce l'elenco dei piani Insider disponibili. **Accesso:** autenticato

#### `POST /api/v1/membership/buy`
Sottoscrive un piano Insider per l'utente autenticato. **Accesso:** autenticato
```json
{ "planId": 1 }
```

### 14.4 Wallet

#### `POST /api/v1/wallet/recharge`
```json
{ "amount": 50.00 }
```

#### `GET /api/v1/wallet/mywallet`
Restituisce il wallet associato all'utente autenticato.

### 14.5 Catalogo

#### `GET /api/v1/products`
Restituisce i prodotti disponibili e il relativo stock. **Accesso:** autenticato

#### `POST /api/v1/admin/products`
Inserisce un nuovo prodotto. **Accesso:** `ADMIN`
```json
{ "name": "Pennelli professionali set 12pz", "description": "...", "price": 18.90, "stockQuantity": 15, "category": "Materiali" }
```
Un utente autenticato senza ruolo amministrativo riceve `403 Forbidden`.

### 14.6 Acquisti

#### `GET /api/v1/user/shop/history`
Restituisce lo storico degli acquisti dell'utente, dal più recente.

#### `POST /api/v1/shop/checkout`
```json
{ "items": [{ "id": 1, "quantity": 2 }, { "id": 3, "quantity": 1 }] }
```
Risposta:
```json
{ "message": "Acquisto completato con successo!", "totalSpent": 12.48, "newBalance": 222.25 }
```
`400 Bad Request` per carrello/quantità non validi o stock/saldo insufficiente. `409 Conflict` per aggiornamento concorrente in conflitto.

#### `POST /api/v1/shop/cart/preview`
Calcola il totale del carrello con e senza sconto Insider applicato, senza effettuare l'acquisto. **Accesso:** autenticato
```json
{ "items": [{ "id": 1, "quantity": 2 }, { "id": 3, "quantity": 1 }] }
```
Risposta:
```json
{ "originalTotal": 15.60, "discountedTotal": 12.48, "discountPercentage": 20 }
```
---

## 15. Verifica e test

```bash
./mvnw test
./mvnw clean verify
```

`mvn verify` applica la soglia minima di copertura JaCoCo: **80% instruction coverage**. Report HTML in `target/site/jacoco/index.html`.

I test utilizzano il database H2 in-memory configurato in `src/test/resources/application.properties`.

### 15.1 Test di integrazione

| Test | Verifica |
| ---- | ------- |
| `MassAssignmentIntegrationTest` | Il campo `role` inviato durante la registrazione viene ignorato: l'account creato è sempre `USER`. |
| `AdminAccessControlIntegrationTest` | Un utente `USER` riceve `403` su `POST /api/v1/admin/products`; lo stesso utente promosso ad `ADMIN` nel database riceve `201`. |
| `RefreshTokenRotationIntegrationTest` | Un refresh token già utilizzato non può essere riutilizzato, sia tramite `/api/v1/auth/refresh` sia dopo il refresh silenzioso eseguito dal `JwtFilter`. |
| `RateLimitingIntegrationTest` | Dopo 5 richieste in un minuto a `/api/v1/artwork/featured`, la sesta riceve `429`. Il limiter conta la richiesta prima della chiamata esterna, quindi il comportamento è verificabile anche se l'API ARTIC non è raggiungibile. |
| `CheckoutTransactionIntegrationTest` | Un checkout rifiutato per saldo insufficiente non modifica lo stock. |
| `ConcurrentCheckoutIntegrationTest` | Due checkout concorrenti sull'ultimo pezzo disponibile: ne viene completato uno solo e lo stock finale è `0`. |
| `UserApiEndpointsIntegrationTest` | Flussi dell'utente autenticato: lettura e ricarica del wallet, elenco di prodotti e piani, acquisto di un piano Insider, registrazione dell'acquisto nello storico, invalidazione del refresh token al logout. |
| `ArteViaApplicationIntegrationTests` | Avvio corretto del contesto Spring. |

Le classi di integrazione sono basate su `@SpringBootTest` e `MockMvc`.

### 15.2 Test unitari dei servizi (Mockito)

* `WalletServiceTest` - rifiuto di importi negativi o pari a zero, ricarica valida, errore in assenza di wallet;
* `ShopServiceTest` - prodotto inesistente, stock insufficiente, saldo insufficiente senza modifica dello stock, applicazione dello sconto Insider con aggiornamento di stock e saldo, nessuno sconto con abbonamento scaduto, rifiuto di quantità che causerebbero overflow, totale addebitato uguale alla somma dei prezzi unitari registrati nello storico;
* `MembershipServiceTest` - piano inesistente, saldo insufficiente senza creazione dell'abbonamento, disattivazione dell'abbonamento precedente e addebito del nuovo piano.

### 15.3 Test di concorrenza

`ConcurrentCheckoutIntegrationTest` verifica uno scenario con stock disponibile = 1: due richieste eseguite su un pool di due thread e fatte partire nello stesso istante da un `CountDownLatch` di partenza; un secondo latch attende la fine di entrambe. Una sola richiesta viene completata; l'altra viene rifiutata, per conflitto di versione rilevato da `@Version` (`409`) oppure per stock insufficiente se legge lo stock già aggiornato (`400`). Lo stock finale è sempre `0` e non diventa mai negativo.

### 15.4 Test manuali con Postman

La collection `postman/ArteVia.postman_collection.json` contiene 81 richieste con verifiche automatiche su: registrazione e mass assignment, attacchi al JWT (payload manomesso, `alg: none`, firma con chiave diversa), rotazione e riuso dei refresh token, refresh silenzioso, logout, validazione degli input, manomissione di prezzi e quantità, atomicità del checkout, sconto Insider, controllo degli accessi, rate limiting per utente, gestione degli errori e header di sicurezza.

1. In Postman: *Settings → General → SSL certificate verification* disattivato (certificato self-signed);
2. *Import* del file della collection;
3. con l'applicazione avviata e i dati iniziali caricati, eseguire le cartelle da `00` a `09` in ordine con il *Collection Runner*;
4. le richieste della cartella `10` vanno eseguite singolarmente, dopo aver lanciato la query SQL indicata nella descrizione di ciascuna.

Ogni esecuzione crea utenti con nomi univoci, quindi la collection può essere rieseguita senza svuotare il database.

### 15.5 Verifica manuale dell'escaping

La protezione dei dati dinamici visualizzati dal frontend utilizza `escapeHtml()` (e `th:text` di Thymeleaf per lo username). Verifica creando, tramite account amministratore, un prodotto con nome `<img src=x onerror=alert(1)>` (richiesta `10.4` della collection), poi aprendo `/home/shop`: il contenuto deve comparire come testo, non come codice HTML eseguito.

---

## 16. Stato del progetto

ArteVia è pensato come progetto didattico per lo studio congiunto di: sviluppo web con Spring Boot, autenticazione e autorizzazione, sicurezza delle API REST, gestione transazionale, concorrenza a livello database, integrazione con API esterne, testing automatico delle proprietà di sicurezza.

Le credenziali, il certificato HTTPS e le configurazioni incluse nel repository sono esclusivamente destinate all'esecuzione locale e alla valutazione del progetto.

## 17. Glossario dei concetti di sicurezza

Concetti di sicurezza adottati, con il modo in cui sono applicati e la loro collocazione nel codice.

| Concetto | Descrizione e applicazione in ArteVia | Riferimenti |
| --- | --- | --- |
| **Hashing delle password (BCrypt)** | Le password non vengono memorizzate in chiaro: si salva un hash con salt, non reversibile, e a ogni login si confronta l'hash.<br>**In ArteVia:** `BCryptPasswordEncoder`, usato in registrazione e in login tramite `DaoAuthenticationProvider`. La password richiesta ha 8-72 caratteri (72 è il limite di BCrypt, espresso in byte) e deve contenere maiuscola, minuscola, cifra e carattere speciale. | `WebSecurityConfig.passwordEncoder()`, `AuthService.register()`, `AuthService.login()`, `RegisterRequest` |
| **JWT (access token)** | Token firmato che il server verifica (firma e scadenza) senza consultare il database.<br>**In ArteVia:** firma HMAC con la chiave `jwt.secret`; contiene solo lo username (subject), la data di emissione e la scadenza (15 minuti); **non contiene il ruolo**. Non viene revocato al logout: resta valido fino alla scadenza. | `JwtService`, `application.properties` (`jwt.*`) |
| **Autenticazione stateless** | Il server non mantiene sessioni: ogni richiesta porta con sé le proprie credenziali.<br>**In ArteVia:** sessioni `STATELESS`; `JwtFilter` ricostruisce il `SecurityContext` a ogni richiesta leggendo il token dall'header `Authorization: Bearer` (ha la precedenza) o dal cookie `accessToken`, e ricarica utente e ruolo dal database. | `WebSecurityConfig.filterChain()`, `JwtFilter`, `UserDetailsServiceImpl.loadUserByUsername()` |
| **Refresh token e rotazione** | Credenziale a lunga durata usata solo per ottenere nuovi access token. La rotazione la rende monouso: a ogni utilizzo viene invalidata e sostituita, quindi un token già usato non può essere riutilizzato.<br>**In ArteVia:** stringa casuale (UUID) salvata nel database con scadenza a 30 giorni; a ogni uso viene cancellata e ne viene emessa una nuova; il logout la cancella. Un token sconosciuto o scaduto viene rifiutato con `400` su `/api/v1/auth/refresh` e con `401` nel refresh silenzioso. Se due richieste parallele usano lo stesso token, solo la prima lo ruota: nel refresh silenzioso l'altra riceve `401`, su `/api/v1/auth/refresh` riceve `409` o `400`. | `AuthService.refresh()`, `AuthService.logout()`, `RefreshTokenService.silentRefresh()`, `JwtFilter.silentRefresh()`, `RefreshToken` |
| **Refresh silenzioso** | Rinnovo trasparente delle credenziali: se l'access token è scaduto ma il refresh token è valido, il server ne emette di nuovi senza un nuovo login.<br>**In ArteVia:** `JwtFilter` lo esegue per le richieste esterne a `/api/v1/auth/**`, usando il cookie `refreshToken`, e scrive i nuovi cookie nella risposta. | `JwtFilter.silentRefresh()`, `RefreshTokenService.silentRefresh()` |
| **HTTPS / TLS** | Cifratura e autenticazione del canale tra client e server.<br>**In ArteVia:** l'applicazione risponde solo in HTTPS sulla porta 8443, con keystore PKCS12; il certificato incluso è self-signed, destinato allo sviluppo. | `application.properties` (`server.ssl.*`), `src/main/resources/keystore.p12` |
| **Cookie `HttpOnly`, `Secure`, `SameSite`** | `HttpOnly`: il cookie non è leggibile da JavaScript (riduce l'impatto di un XSS). `Secure`: viene inviato solo su HTTPS. `SameSite=Lax`: il browser non lo invia nei POST provenienti da altri siti.<br>**In ArteVia:** entrambi i cookie di autenticazione (`accessToken`, `refreshToken`) hanno i tre attributi, con `path=/`; il logout li azzera con `maxAge=0`. | `CookieUtils.addAuthCookie()`, `AuthController`, `JwtFilter` |
| **CSRF** | Attacco in cui un sito terzo induce il browser della vittima a inviare richieste autenticate dai cookie.<br>**In ArteVia:** la protezione CSRF di Spring è disabilitata per scelta progettuale: ci si affida a `SameSite=Lax` e al fatto che le API accettano corpi JSON. | `WebSecurityConfig.filterChain()` |
| **CORS** | Regola del browser che stabilisce quali altre origini possono chiamare le API.<br>**In ArteVia:** ammesse `http://localhost:5500` e `http://127.0.0.1:5500` (metodi `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, credenziali incluse) per un eventuale frontend separato in sviluppo; il frontend Thymeleaf integrato usa la stessa origine e non ne ha bisogno. | `WebSecurityConfig.corsConfigurationSource()` |
| **Header di sicurezza HTTP** | Intestazioni di risposta che istruiscono il browser (per esempio `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`).<br>**In ArteVia:** non sono configurate a mano: sono quelle predefinite di Spring Security e vengono controllate dalla collection Postman. | `WebSecurityConfig`, Postman: cartella `09 - Errori, header e infrastruttura` |
| **Controllo degli accessi (RBAC) e default-deny** | Autorizzazione in base al ruolo; con il "default-deny" tutto ciò che non è dichiarato pubblico richiede autenticazione.<br>**In ArteVia:** ruoli `USER` e `ADMIN`; sono pubbliche solo le rotte elencate in `permitAll`, il resto è `authenticated()`; gli endpoint amministrativi usano `@PreAuthorize("hasRole('ADMIN')")` (method security abilitata). Il ruolo è letto dal database a ogni richiesta ed è sempre `USER` alla registrazione. Senza autenticazione: `401` (JSON su `/api/**` e `/actuator/**`, redirect a `/auth/login` per le pagine); con ruolo insufficiente: `403`. | `WebSecurityConfig.filterChain()`, `AdminController.addProduct()`, `UserDetailsImpl.getAuthorities()`, `Role` |
| **Mass assignment** | Il client valorizza campi che non dovrebbe poter impostare (per esempio `"role": "ADMIN"` in registrazione).<br>**In ArteVia:** le richieste passano da DTO che non contengono `role`; le entity JPA non sono mai il contratto di input; un campo iniettato viene ignorato. | `RegisterRequest`, `AuthService.register()`, `MassAssignmentIntegrationTest` |
| **Validazione degli input** | Controllo di formato e limiti prima che i dati raggiungano la logica applicativa.<br>**In ArteVia:** Jakarta Validation sui DTO (`@NotBlank`, `@Email`, `@Pattern` sullo username `^[A-Za-z0-9_.-]{3,30}$`, `@Size`, `@DecimalMin`, `@DecimalMax`, `@Digits` sugli importi) con `@Valid` nei controller; le violazioni producono `400` con una mappa campo → messaggio. | package `dto`, `GlobalExceptionHandler.handleValidation()` |
| **SQL injection** | Inserimento di frammenti SQL in un input che viene concatenato in una query.<br>**In ArteVia:** non c'è SQL scritto a mano (nessun `@Query`, query native o `EntityManager`): l'accesso ai dati usa solo repository Spring Data JPA con query derivate dal nome dei metodi, quindi parametrizzate. | package `repository` |
| **Overflow e limiti sulle quantità** | Valori molto grandi possono superare l'intervallo di un intero e diventare negativi o nulli, falsando i totali.<br>**In ArteVia:** quantità per prodotto `@Max(100)`, carrello di al massimo 50 righe (`@Size(max = 50)`), righe duplicate unite con `Math.addExact`; un overflow lancia `ArithmeticException`, tradotta in `400`. | `CartItemRequest`, `CheckoutRequest`, `ShopService.mergeByProduct()`, `GlobalExceptionHandler` |
| **Ricalcolo lato server (client non attendibile)** | Prezzi, sconti e disponibilità ricevuti dal client non sono affidabili: vanno riletti dalla fonte autorevole.<br>**In ArteVia:** il client invia solo id prodotto e quantità; il server rilegge prodotto, prezzo e stock dal database, verifica l'abbonamento Insider (attivo e con `endDate` futura) per applicare lo sconto, calcola il totale e controlla il saldo. | `ShopService.checkout()`, `ShopService.previewCart()` |
| **Transazione** | Gruppo di operazioni sul database che riescono tutte o nessuna.<br>**In ArteVia:** `@Transactional` su registrazione (utente e wallet), rotazione dei refresh token, ricarica, acquisto di un piano e checkout (stock, saldo e storico acquisti si aggiornano insieme). | `ShopService.checkout()`, `MembershipService.buyPlan()`, `WalletService.recharge()`, `AuthService.register()`, `AuthService.refresh()` |
| **Optimistic locking e lost update** | Se due operazioni modificano la stessa riga nello stesso momento, la seconda può sovrascrivere la prima senza accorgersene (lost update). Con l'optimistic locking ogni riga ha un numero di versione: se è cambiato, l'aggiornamento fallisce.<br>**In ArteVia:** `@Version` su `Product` e `Wallet`; il conflitto genera `ObjectOptimisticLockingFailureException`, tradotta in `409`. Il caso di due checkout sull'ultimo pezzo è esercitato da `ConcurrentCheckoutIntegrationTest`. | `Product`, `Wallet`, `GlobalExceptionHandler`, `ConcurrentCheckoutIntegrationTest` |
| **Rate limiting** | Limite al numero di richieste in un intervallo di tempo.<br>**In ArteVia:** Resilience4j, con un limiter per utente e per endpoint (`articApi-<username>`, `articImage-<username>`) e configurazione condivisa `articApi` (5 richieste ogni 60 secondi, senza attesa). Si applica agli endpoint dell'opera in evidenza e dell'immagine; oltre il limite la risposta è `429`. | `ApiController.featuredArtwork()`, `ApiController.artworkImage()`, `application.properties` (`resilience4j.ratelimiter.*`), `GlobalExceptionHandler` |
| **SSRF e proxy delle immagini** | Un server che scarica URL per conto del client può essere indotto a contattare destinazioni non volute (SSRF).<br>**In ArteVia:** l'endpoint proxy accetta solo un `imageId` che rispetta `^[a-zA-Z0-9-]{10,60}$` e lo inserisce in un URL con host fisso (`www.artic.edu`); il server scarica l'immagine e la inoltra al client, evitando blocchi cross-origin (Opaque Response Blocking) e restrizioni anti-hotlink del CDN. | `ArtworkOfDayService.fetchImageBytes()`, `ApiController.artworkImage()` |
| **Timeout e indisponibilità dei servizi esterni** | Le chiamate a servizi terzi hanno un tempo massimo, per non bloccare l'applicazione se il servizio non risponde.<br>**In ArteVia:** `WebClient` con timeout di connessione (5 secondi) e di risposta, lettura e scrittura (8 secondi); un errore del servizio esterno diventa `ExternalServiceUnavailableException`, tradotta in `503`. | `WebClientConfig`, `ArtworkOfDayService`, `GlobalExceptionHandler` |
| **XSS ed escaping** | Testo con codice (per esempio `<script>`) inserito da un utente e poi eseguito nel browser di altri. Si previene trattando ogni dato dinamico come testo.<br>**In ArteVia:** i valori scritti nella pagina con `innerHTML` passano da `escapeHtml()`; lo username è reso con `th:text` di Thymeleaf. La verifica manuale usa un prodotto con nome `<img src=x onerror=alert(1)>` (richiesta Postman `10.4`). | `templates/home/shop.html`, `templates/home/profile.html`, `templates/auth/login.html`, `templates/auth/register.html` |
| **Gestione centralizzata degli errori** | Un unico punto traduce le eccezioni in risposte con formato uniforme.<br>**In ArteVia:** `@RestControllerAdvice` che restituisce `{"error": "..."}` con il codice HTTP appropriato (`400`, `401`, `403`, `409`, `429`, `503`, ...). | `GlobalExceptionHandler` |
| **Gestione dei segreti e della configurazione** | Credenziali e chiavi non vanno scritte nel codice: si leggono dall'ambiente.<br>**In ArteVia:** `DB_PASSWORD` e `JWT_SECRET` sono lette dalle variabili d'ambiente, con valori di default validi solo per lo sviluppo; `.env.example` è il modello dei valori da impostare (Spring Boot non lo legge automaticamente). | `application.properties`, `.env.example` |
| **Superficie esposta (Actuator)** | Gli endpoint di monitoraggio devono esporre solo ciò che serve.<br>**In ArteVia:** sono esposti solo `health` e `info`; `/actuator/health` è pubblico, il resto richiede autenticazione. | `application.properties` (`management.endpoints.web.exposure.include`), `WebSecurityConfig.filterChain()` |