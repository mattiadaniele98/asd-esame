# Automated Software Delivery — Cosa abbiamo fatto e perché

---

## Punto di partenza

Il progetto di base è **materie-service**: un microservizio Java Spring Boot che gestisce il catalogo delle materie scolastiche. Aveva già:
- Maven (`pom.xml`) per la gestione delle dipendenze e la build
- Docker e Docker Compose per l'esecuzione in container
- Test unitari con Mockito

**Cosa mancava:** analisi statica del codice, pipeline CI e gestione corretta dei branch.

---

## 1. Analisi statica — i 4 plugin nel `pom.xml`

L'analisi statica è la pratica di esaminare il codice **senza eseguirlo** per trovare problemi di qualità. Abbiamo aggiunto 4 plugin nel blocco `<build><plugins>` del `pom.xml`:

---

### JaCoCo — Coverage dei test

**Cosa fa:** strumenta il bytecode e misura quanti branch del codice vengono effettivamente eseguiti dai test. Se la copertura scende sotto la soglia configurata, **fa fallire la build**.

**Perché è importante:** un criterio di adeguatezza è un predicato vero/falso su ⟨programma, test suite⟩ — JaCoCo lo verifica automaticamente ad ogni build.

**Come è configurato:**
```xml
<counter>BRANCH</counter>
<value>COVEREDRATIO</value>
<minimum>0.70</minimum>   <!-- soglia: 70% dei branch devono essere coperti -->
```

**Tre execution in sequenza:**
1. `prepare-agent` → strumenta il bytecode prima dei test
2. `report` → genera il report HTML in `target/site/jacoco/`
3. `check` → fa fallire la build se la soglia non è soddisfatta

**Perché branch coverage e non line coverage?**
La branch coverage (criterio C2) è più esigente: richiede che ogni possibile uscita da ogni decisione (`if/else`, `&&`, `||`) sia testata almeno una volta. La line coverage (C1) richiede solo che ogni riga sia eseguita — si può avere il 100% di line coverage pur non testando mai il ramo `else`.

---

### Checkstyle — Stile del codice

**Cosa fa:** controlla che il codice rispetti regole di stile definite in un file XML. Può fare fallire la build se ci sono violazioni.

**File di configurazione:** `checkstyle.xml` nella root del progetto.

**Regole principali configurate:**

| Regola | Cosa controlla |
|--------|---------------|
| `AvoidStarImport` | Vieta `import java.util.*` — gli import devono essere espliciti |
| `UnusedImports` | Segnala import dichiarati ma mai usati |
| `NeedBraces` | Le graffe sono obbligatorie anche per `if` con una sola riga |
| `ModifierOrder` | L'ordine dei modificatori deve essere `public abstract static final...` |
| `WhitespaceAround` | Spazi obbligatori intorno agli operatori e dopo le virgole |
| `CyclomaticComplexity` | Massimo 10 percorsi indipendenti per metodo |
| `LineLength` | Massimo 120 caratteri per riga |

**Perché la complessità ciclomatica massima è 10?**
McCabe stesso ha suggerito 10 come soglia: oltre questo valore il codice diventa difficile da testare e mantenere (troppi percorsi da coprire con i test).

**Differenza con PMD:** Checkstyle guarda lo **stile e la formattazione**, PMD guarda le **bad practice logiche e di design**.

---

### PMD — Bad practice

**Cosa fa:** analisi statica cross-language che cerca pattern problematici nel codice sorgente: codice duplicato, variabili inutilizzate, design problematico, possibili bug.

**Categorie di controllo:**
- Qualità interna: Code Style, Design, Documentation
- Bug: Best practices, Error Prone, Multithreading
- Problemi non funzionali: Security, Performance

---

### SpotBugs — Bug nel bytecode

**Cosa fa:** analizza direttamente il **bytecode** (non il sorgente) cercando pattern noti di bug. Più di 400 pattern predefiniti.

**Perché analizza il bytecode e non il sorgente?** È più veloce e cattura problemi che emergono solo dopo la compilazione (es. operazioni su tipi primitivi, boxing/unboxing automatico).

**Esempi di bug rilevabili:**
- `new Integer(1).toString()` invece di `Integer.toString(1)`
- Metodo che non controlla argomenti null
- Password hardcodata nel codice

**Differenza con Checkstyle:** Checkstyle → stile; SpotBugs → bug potenziali nel bytecode.

---

## 2. `checkstyle.xml` — le regole

Il file `checkstyle.xml` è necessario perché Checkstyle non sa quali regole applicare senza una configurazione esplicita. È strutturato come un documento XML con due livelli:

- `<module name="Checker">` → regole a livello di file (es. lunghezza riga)
- `<module name="TreeWalker">` → regole a livello di codice (analizza l'AST del sorgente)

---

## 3. Pipeline CI con GitHub Actions

**Cos'è la CI (Continuous Integration)?**
La pratica di integrare le modifiche fatte da diversi sviluppatori man mano che vengono completate, anziché aspettare che tutte le parti siano pronte.

**File:** `.github/workflows/ci.yml`

**Trigger:** la pipeline parte ad ogni `push` e ad ogni `pull_request` su qualsiasi branch.

**Struttura del job:**

```
ubuntu-latest (VM pulita fornita da GitHub)
  │
  ├── Servizio PostgreSQL (container avviato prima degli step)
  │
  ├── Step 1: Checkout del codice
  ├── Step 2: Setup Java 21 (Temurin, con cache Maven)
  └── Step 3: mvn verify
              │
              ├── validate    → Checkstyle
              ├── compile     → compilazione sorgente
              ├── test        → test + JaCoCo report
              ├── package     → creazione JAR
              └── verify      → JaCoCo check + PMD + SpotBugs
```

**Perché il servizio PostgreSQL nel CI?**
Il test `MaterieServiceApplicationTests` usa `@SpringBootTest` che avvia il contesto Spring completo e tenta di connettersi al DB. Senza PostgreSQL, il test fallirebbe. Il `--health-cmd pg_isready` garantisce che Maven parta solo quando il DB è pronto.

**Perché `cache: 'maven'`?**
Evita di riscaricare tutte le dipendenze da Maven Central ad ogni run (risparmio di tempo e banda).

**Cos'è un job?** Un'esecuzione concreta della pipeline. Ogni commit genera un nuovo job che compare nel tab Actions di GitHub con esito verde (Pass) o rosso (Fail).

---

## 4. Il processo — cosa è successo commit per commit

### Prima run: Fail per Checkstyle (18 violazioni)

La pipeline ha trovato violazioni reali nel codice:

| Violazione | File | Problema |
|-----------|------|---------|
| `NeedBraces` | `MateriaService.java` | `if` senza graffe (5 occorrenze) |
| `AvoidStarImport` | Controller e Entity | `import org.springframework.web.bind.annotation.*` |
| `WhitespaceAround` | DTO records | `{}` corpo vuoto senza spazi |
| `LineLength` | `SecurityConfig.java` | Righe più lunghe di 120 caratteri |

**Questo è il comportamento corretto**: la CI ha rilevato problemi reali e ha bloccato la build. Nel secondo commit abbiamo corretto tutte le 18 violazioni.

**Esempio della correzione NeedBraces:**
```java
// Prima (violazione)
if (dto.nome() != null)  materia.setNome(dto.nome());

// Dopo (corretto)
if (dto.nome() != null) {
    materia.setNome(dto.nome());
}
```

**Perché le graffe sono obbligatorie anche per un'istruzione sola?**
Senza graffe, aggiungere una seconda istruzione sotto l'`if` sembra funzionare ma in realtà la seconda riga viene sempre eseguita indipendentemente dalla condizione. È una fonte classica di bug.

---

### Seconda run: Fail per JaCoCo (coverage 37% < 70%)

La soglia era al 70% ma i test esistenti coprivano solo il 37% dei branch. Mancavano completamente:
- `MateriaService.aggiornaMateria` — 10 branch non coperti (5 `if` null-check, ognuno con ramo vero e falso)
- `MateriaService.eliminaMateria` — 4 branch non coperti (not found, soft delete, hard delete)
- `MateriaClasseService` — **0% coperto**: nessun test per l'intera classe (assegna, trova, rimuovi)
- Un branch mancante in `creaMateria` (nome duplicato: codice OK ma nome già esiste)

---

### Terza run: Fail per JaCoCo (coverage 65% < 70%)

Abbiamo aggiunto test per tutte le classi service mancanti:

| File | Test aggiunti |
|------|--------------|
| `MateriaServiceTest` | +7 test: nome duplicato, aggiornaMateria (3 casi), eliminaMateria (3 casi) |
| `MateriaClasseServiceTest` | Nuovo — 7 test: assegnaMateria (3 casi), trovaPerClasse (2), rimuoviAssegnazione (2) |
| `MateriaControllerTest` | +2 test: aggiornaMateria 200, eliminaMateria 204 |

La coverage è salita da 37% a **65%**. Mancavano ancora i branch nei filtri HTTP.

---

### Quarta run (attesa): Pass — coverage ≥ 70%

Abbiamo aggiunto test per `JwtAuthFilter` e `VersioningFilter`:

| File | Test aggiunti | Branch coperti |
|------|--------------|----------------|
| `security/JwtAuthFilterTest` | 6 test | `shouldNotFilter` true/false, header assente, header non-Bearer, token invalido, token valido |
| `filter/VersioningFilterTest` | 4 test | forward per `/materie`, forward con sotto-path, forward per `/materie-classe`, path sconosciuto |

---

## 5. Gestione branch

**Regola fondamentale:** non si lavora mai direttamente su `main`.

**Branch `main`:** contiene il codice stabile e verificato dalla CI. Build sempre verde.

**Branch `sviluppo`:** branch di lavoro dove si aggiungono i test e si porta la coverage al 70%.

```
main      ──●──────────────────────────────●──→  (stabile, sempre verde)
              \                            /
sviluppo       ●──●──●──●──●──●──●──●────●   (lavoro in corso)
```

**Pull Request:** nella pratica professionale, il passaggio da `sviluppo` a `main` avviene tramite pull request — un maintainer rivede il codice e la CI deve essere verde prima del merge.

---

## 6. Come abbiamo raggiunto il 70% con soli test unitari

### Il problema dei filtri

I filtri HTTP (`JwtAuthFilter`, `VersioningFilter`) estendono `OncePerRequestFilter` di Spring, che ha metodi `protected`:
- `shouldNotFilter(HttpServletRequest request)` → decide se il filtro va saltato
- `doFilterInternal(request, response, chain)` → logica reale del filtro

Questi metodi non sono accessibili da fuori con test normali. Abbiamo usato due tecniche:

---

### Tecnica 1 — Stesso package per accedere ai metodi `protected`

In Java, `protected` significa: accessibile dalla stessa classe, dalle sottoclassi **e dalle classi nello stesso package**.

Mettendo il test nello stesso package del filtro:
```
src/test/java/it/scuola/materie_service/security/JwtAuthFilterTest.java
                                                  ^^^^^^^
                                          stesso package di JwtAuthFilter
```

...il test può chiamare direttamente `jwtAuthFilter.shouldNotFilter(request)` e `jwtAuthFilter.doFilterInternal(request, response, chain)` senza trucchi.

---

### Tecnica 2 — `ReflectionTestUtils` per i campi `@Value`

`VersioningFilter` legge la configurazione da `application.properties` tramite `@Value`:
```java
@Value("${forward.version.from}")
private List<String> versionFrom;
```

Mockito con `@InjectMocks` non inietta i campi `@Value` (quello fa Spring, non Mockito). Soluzione:
```java
ReflectionTestUtils.setField(versioningFilter, "versionFrom", List.of("/materie", "/materie-classe"));
ReflectionTestUtils.setField(versioningFilter, "versionTo",   List.of("/api/v1/materie", "/api/v1/materie-classe"));
```

`ReflectionTestUtils` è una classe di Spring Test che usa la reflection Java per impostare campi privati/protected direttamente — bypassa l'iniezione normale.

---

### Tecnica 3 — `SecurityContextHolder` funziona senza Spring

`JwtAuthFilter` scrive nel `SecurityContextHolder` di Spring Security:
```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

Nei test unitari, `SecurityContextHolder` funziona normalmente perché usa un `ThreadLocal` — non ha bisogno del contesto Spring. Puliamo il context dopo ogni test con:
```java
@AfterEach
void clearSecurityContext() {
    SecurityContextHolder.clearContext();
}
```

Senza questa pulizia, l'autenticazione del test precedente rimarrebbe nel `ThreadLocal` e interferirebbe con i test successivi.

---

### Perché i mock di `HttpServletRequest` funzionano?

Mockito crea un oggetto finto che restituisce valori di default sicuri per tutti i metodi:
- `getAttribute(...)` → `null` (quindi il filtro non è "già stato eseguito")
- `getHeader(...)` → controllato dal test con `when(request.getHeader(...)).thenReturn(...)`
- `getRequestURI()` → controllato dal test

Questo è sufficiente per testare tutta la logica condizionale dei filtri.

---

## 7. I branch nel dettaglio — cosa conta JaCoCo

JaCoCo misura i **branch**, non le righe. Un branch è ogni possibile uscita da una decisione:

| Costrutto | Branch generati |
|-----------|----------------|
| `if (a)` | 2: a=true, a=false |
| `if (a \|\| b)` | 3: a=true (cortocircuito), a=false+b=true, a=false+b=false |
| `if (a && b)` | 3: a=false (cortocircuito), a=true+b=false, a=true+b=true |
| `a ? b : c` | 2: condizione vera, condizione falsa |
| `try/catch` | 2: nessuna eccezione, eccezione lanciata |

**Esempio concreto da `aggiornaMateria`:**
```java
if (dto.nome() != null) {        // branch 1: null, branch 2: non-null
    materia.setNome(dto.nome());
}
if (dto.descrizione() != null) { // branch 3: null, branch 4: non-null
    materia.setDescrizione(dto.descrizione());
}
// ... altri 3 if simili → altri 6 branch
```

Per coprire tutti i branch di `aggiornaMateria` bastano 2 test:
- Uno con tutti i campi valorizzati → tutti i branch "non-null" vengono eseguiti
- Uno con tutti i campi null → tutti i branch "null" vengono eseguiti

---

## Riepilogo finale

| Cosa | Strumento | Dove | Perché |
|------|----------|------|--------|
| Coverage dei test | JaCoCo | `pom.xml` | Misura e impone criteri di adeguatezza (≥70% branch) |
| Stile del codice | Checkstyle | `pom.xml` + `checkstyle.xml` | Mantiene leggibilità e coerenza |
| Bad practice | PMD | `pom.xml` | Trova design problematici nel sorgente |
| Bug nel bytecode | SpotBugs | `pom.xml` | Trova pattern di bug dopo la compilazione |
| Integrazione continua | GitHub Actions | `.github/workflows/ci.yml` | Esegue tutto ad ogni commit automaticamente |
| Branch | Git | Repository GitHub | Separa codice stabile da sviluppo in corso |

### Test file aggiunti/modificati

| File | Tipo | Tecnica usata |
|------|------|---------------|
| `MateriaServiceTest` | Unitario Mockito | `@Mock` + `@InjectMocks` |
| `MateriaClasseServiceTest` | Unitario Mockito | `@Mock` + `@InjectMocks` |
| `MateriaControllerTest` | Unitario Mockito | `@Mock` + `@InjectMocks` |
| `security/JwtAuthFilterTest` | Unitario Mockito | Stesso package → accesso a `protected` |
| `filter/VersioningFilterTest` | Unitario Mockito | Stesso package + `ReflectionTestUtils` per `@Value` |
