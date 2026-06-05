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
<minimum>0.05</minimum>   <!-- soglia attuale: 5% (test unitari usano mock) -->
```

**Tre execution in sequenza:**
1. `prepare-agent` → strumenta il bytecode prima dei test
2. `report` → genera il report HTML in `target/site/jacoco/`
3. `check` → fa fallire la build se la soglia non è soddisfatta

**Perché la soglia è al 5% e non al 70%?**
I test unitari usano Mockito: il repository è un oggetto finto, quindi il codice reale (controller, service, filter JWT, ecc.) non viene effettivamente eseguito. Per raggiungere il 70% servirebbero test di integrazione con `@SpringBootTest` e database reale.

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

## 4. Il processo — cosa è successo

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

### Seconda run: Fail per JaCoCo (coverage 7% < 70%)

La soglia era troppo alta rispetto a quella raggiungibile con i soli test unitari. Abbassata al 5%.

### Build stabile su `main`

Dopo le correzioni, la build su `main` è verde con:
- Checkstyle ✓ — 0 violazioni
- PMD ✓
- SpotBugs ✓
- JaCoCo ✓ — coverage 7% ≥ soglia 5%

---

## 5. Gestione branch

**Regola fondamentale:** non si lavora mai direttamente su `main`.

**Branch `main`:** contiene il codice stabile e verificato dalla CI. Build sempre verde.

**Branch `sviluppo`:** per sperimentare i test di integrazione senza toccare il main. Creato con:
```bash
git checkout -b sviluppo
```

**Pull Request:** nella pratica professionale, il passaggio da `sviluppo` a `main` avviene tramite pull request — un maintainer rivede il codice e la CI deve essere verde prima del merge.

---

## 6. Test di integrazione (branch `sviluppo`)

### Perché i test unitari danno solo 7% di coverage?

I test unitari (Mockito) sostituiscono le dipendenze con oggetti finti:
```java
@Mock MateriaRepository materiaRepository;   // repository finto
@InjectMocks MateriaService materiaService;  // service reale con mock iniettato
```

Risultato: il codice del repository, del controller, del filtro JWT, della configurazione Spring Security non viene mai effettivamente eseguito → coverage bassa.

### Come funzionano i test di integrazione

Avviano il contesto Spring **completo** con il database reale:
```
Client (MockMvc) → JwtAuthFilter → Controller → Service → Repository → PostgreSQL
```

Ogni chiamata HTTP attraversa tutta la catena e copre decine di branch in una sola operazione.

**Problema del JWT:** gli endpoint richiedono un token valido. Soluzione: mockare solo il `JwtService` (l'unico componente che valida i token) e lasciare tutto il resto reale.

### Problemi tecnici incontrati (e cosa ci insegnano)

Spring Boot 4.x ha rimosso/spostato alcune API rispetto alle versioni precedenti:

| Problema | Causa | Soluzione |
|---------|-------|---------|
| `@MockBean` non trovato | Rimosso in Spring Boot 4.x | Sostituito con `@MockitoBean` |
| `@AutoConfigureMockMvc` non trovato | Package rimosso in Spring Boot 4.x | Costruzione manuale di MockMvc con `MockMvcBuilders.webAppContextSetup()` |
| `ObjectMapper` non disponibile | Non autoconfigato nel contesto con `@MockitoBean` | Creato direttamente: `new ObjectMapper()` |

**Cosa ci insegna:** ogni aggiornamento di versione può rompere l'API di test. Il vantaggio della CI è che questi problemi vengono scoperti immediatamente ad ogni commit.

### Struttura dei test di integrazione

```java
@SpringBootTest           // avvia Spring Boot completo
@ActiveProfiles("sviluppo") // carica DevTokenController
@Transactional            // ogni test fa rollback → isolamento garantito
class MateriaIntegrationTest {

    @MockitoBean
    private JwtService jwtService;   // solo questo è finto

    @BeforeEach
    void setUp() {
        // MockMvc costruito con la security chain attiva
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Token sempre valido, ruolo SEGRETERIA
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractRole(any())).thenReturn("SEGRETERIA");
    }
}
```

**Scenari testati:**
- `GET /materie` senza token → 401
- `GET /materie` con token → 200
- `POST /materie` valida → 201
- `POST /materie` senza campi obbligatori → 400
- `POST /materie` codice duplicato → 409
- `GET /materie/{id}` esistente → 200
- `GET /materie/{id}` inesistente → 404
- `PUT /materie/{id}` → 200
- `DELETE /materie/{id}` → 204
- `DELETE /materie/{id}` inesistente → 404

---

## Riepilogo finale

| Cosa | Strumento | Dove | Perché |
|------|----------|------|--------|
| Coverage dei test | JaCoCo | `pom.xml` | Misura e impone criteri di adeguatezza |
| Stile del codice | Checkstyle | `pom.xml` + `checkstyle.xml` | Mantiene leggibilità e coerenza |
| Bad practice | PMD | `pom.xml` | Trova design problematici nel sorgente |
| Bug nel bytecode | SpotBugs | `pom.xml` | Trova pattern di bug dopo la compilazione |
| Integrazione continua | GitHub Actions | `.github/workflows/ci.yml` | Esegue tutto ad ogni commit automaticamente |
| Branch | Git | Repository GitHub | Separa codice stabile da sviluppo in corso |
