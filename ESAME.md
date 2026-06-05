# Guida Esame — materie-service

---

## 1. Architettura a Microservizi

**Cos'è un microservizio?**
Un'applicazione piccola e indipendente che fa una sola cosa. Nel progetto ci sono 5:
`utenti-service`, `composizione-service`, `materie-service`, `iscrizioni-service`, `attivita-service`.

**Vantaggi:**
- Ogni servizio ha il suo database
- Si aggiornano/deployano indipendentemente
- Un servizio che crasha non blocca gli altri

**Come comunicano?**
REST sincrono — un servizio chiama le API dell'altro via HTTP.

---

## 2. Struttura del Progetto Spring Boot

```
controller/   → riceve le richieste HTTP, parla col client tramite DTO
service/      → logica di business + Repository (come nell'esempio del prof)
model/        → Entity (tabelle DB) + DTO (oggetti scambiati con l'esterno)
security/     → JwtService (valida token) + JwtAuthFilter (filtra richieste)
config/       → SecurityConfig + OpenApiConfig
filter/       → VersioningFilter (conveerte /materie → /api/v1/materie)
exception/    → ResourceNotFoundException + GlobalExceptionHandler
```

---

## 3. Annotazioni Spring Boot da sapere

| Annotazione | Dove si usa | Cosa fa |
|-------------|-------------|---------|
| `@SpringBootApplication` | Classe main | Avvia l'applicazione |
| `@RestController` | Controller | Espone API REST |
| `@RequestMapping("/path")` | Controller (classe) | Prefisso comune di tutti gli endpoint |
| `@GetMapping` | Metodo controller | API GET |
| `@PostMapping` | Metodo controller | API POST |
| `@PathVariable` | Parametro metodo | Legge `{id}` dall'URL |
| `@RequestParam` | Parametro metodo | Legge `?param=valore` dall'URL |
| `@RequestBody` | Parametro metodo | Legge il JSON dal body |
| `@Service` | Classe service | Logica di business |
| `@Repository` | Interfaccia repository | Accesso al database |
| `@Autowired` | Campo | Inietta automaticamente la dipendenza |
| `@Entity` | Classe model | Mappa la classe su una tabella DB |
| `@Table(name="...")` | Classe model | Specifica il nome della tabella |
| `@Id` | Campo entity | Chiave primaria |
| `@GeneratedValue` | Campo entity | Genera il valore automaticamente |
| `@Column` | Campo entity | Colonna nel DB |
| `@Enumerated(EnumType.STRING)` | Campo enum | Salva il nome dell'enum nel DB |
| `@Component` | Classe generica | Bean gestito da Spring |
| `@Configuration` | Classe config | Contiene configurazioni Spring |
| `@Bean` | Metodo in @Configuration | Crea un bean gestito da Spring |
| `@Profile("sviluppo")` | Classe/Bean | Attiva solo con quel profilo Spring |
| `@PreAuthorize` | Metodo controller | Controlla il ruolo prima di eseguire |

---

## 4. Entity e DTO — differenza fondamentale

**Entity** = fotografia del database
```java
@Entity
@Table(name = "materie")
public class Materia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nome;
    // ...
    
    public Materia(MateriaDTO dto) { /* costruttore da DTO */ }
}
```

**DTO** = oggetto scambiato con il client (record Java)
```java
public record MateriaResponseDTO(UUID id, String nome, ...) {
    public MateriaResponseDTO(Materia m) { /* costruttore da Entity */ }
}
```

**Flusso:** Client → DTO → Controller → Service → Entity → DB

---

## 5. Repository

Estende `CrudRepository` (come nelle slide del professore).
Spring genera automaticamente le query dal nome del metodo:

```java
@Repository
public interface MateriaRepository extends CrudRepository<Materia, UUID> {
    boolean existsByCodice(String codice);  // SELECT ... WHERE codice = ?
    boolean existsByNome(String nome);      // SELECT ... WHERE nome = ?
}
```

Metodi gratuiti da CrudRepository:
- `findAll()` → tutti i record
- `findById(id)` → uno per ID
- `save(entity)` → INSERT o UPDATE
- `delete(entity)` → DELETE
- `existsById(id)` → esiste?

---

## 6. JWT — come funziona

**Cos'è il JWT?** Un token firmato che contiene informazioni sull'utente.

**Struttura:** `header.payload.firma`
- Header: algoritmo usato (HS256)
- Payload: dati (email, ruolo, scadenza)
- Firma: garantisce che non sia stato modificato

**Nel progetto:**
1. `utenti-service` emette il token al login
2. Il client manda il token in ogni richiesta: `Authorization: Bearer <token>`
3. `materie-service` valida il token con `JwtService` (stessa chiave segreta)
4. `JwtAuthFilter` intercetta ogni richiesta, valida il token, mette il ruolo nel SecurityContext

```java
// Come si legge il ruolo dal token
String role = jwtService.extractRole(token); // es: "SEGRETERIA"
```

---

## 7. Spring Security

**SecurityConfig** fa tre cose:
1. Disabilita CSRF (non serve nelle API REST)
2. Sessione STATELESS (ogni richiesta porta il JWT, nessuna sessione server)
3. Richiede autenticazione per tutti gli endpoint

```java
@PreAuthorize("hasAnyRole('SEGRETERIA', 'ADMIN', 'SUPER_ADMIN')")
public ResponseEntity<...> creaMateria(...) { ... }
```

**Codici HTTP da sapere:**

| Codice | Nome | Quando viene restituito |
|--------|------|-------------------------|
| `200` | OK | GET riuscita, PUT riuscita |
| `201` | Created | POST riuscita (risorsa creata) |
| `204` | No Content | DELETE riuscita (nessun body nella risposta) |
| `400` | Bad Request | Dati del body non validi (`@NotBlank` violato, ecc.) |
| `401` | Unauthorized | Token JWT mancante o firma non valida |
| `403` | Forbidden | Token valido ma ruolo non autorizzato (`@PreAuthorize` fallito) |
| `404` | Not Found | Risorsa non trovata nel DB (`ResourceNotFoundException`) |
| `409` | Conflict | Duplicato — stessa materia o stessa assegnazione già esistente |

**Differenza 401 vs 403:**
- `401` → non so chi sei (token assente o falso)
- `403` → so chi sei, ma non puoi farlo (ruolo insufficiente)

---

## 8. Versioning Filter

Il client chiama `/materie`, il controller risponde su `/api/v1/materie`.
Il `VersioningFilter` fa la conversione in modo trasparente:

```
Client → GET /materie
              ↓ VersioningFilter
         GET /api/v1/materie → Controller
```

Configurato in `application.properties`:
```properties
forward.version.from=/materie, /materie-classe
forward.version.to=/api/v1/materie, /api/v1/materie-classe
```

---

## 9. Swagger / OpenAPI

**Cos'è Swagger?** Documentazione automatica delle API.

**Come funziona:**
- Springdoc legge le annotazioni del codice all'avvio
- Genera un JSON su `/v3/api-docs`
- Lo visualizza graficamente su `/swagger-ui/index.html`

**Annotazioni usate:**
```java
@Tag(name = "Materie")                    // raggruppa le API
@Operation(summary = "Crea una materia") // descrive l'endpoint
@ApiResponses({
    @ApiResponse(responseCode = "201"),   // documenta i codici HTTP
    @ApiResponse(responseCode = "403")
})
```

**OpenApiConfig** aggiunge il pulsante "Authorize" per inserire il token JWT e testare le API protette.

---

## 10. Docker

**Dockerfile** — costruisce l'immagine del servizio:
```
Stage 1: Maven compila il codice → produce un JAR
Stage 2: JRE esegue il JAR (immagine più leggera)
```

**Porte — porta interna vs porta esposta:**

Ogni container Docker ha la propria interfaccia di rete virtuale con il proprio IP.
Nel progetto tutti i microservizi usano la **stessa porta interna 9000** (`server.port=9000`).
Docker espone ciascuno sulla **porta 80** dell'host (`"80:9000"`).

```
docker-compose.yml:
  ports:
    - "80:9000"     # porta host : porta container
                    # fuori dal container → dentro il container
```

Poiché ogni container ha un IP virtuale diverso, tutti possono usare la porta 9000 internamente senza conflitti. Dal browser si raggiunge sempre `http://localhost:80` indipendentemente da quale servizio si sta sviluppando.

I servizi si chiamano tra loro usando il nome del container sulla porta 9000:
`http://utenti-service:9000/api/v1/utenti`

**Perché 9000 e non 8080 o altro?**
Scelta arbitraria del gruppo per uniformità. L'unico vincolo è evitare porte sotto la 1024 (riservate al sistema operativo, richiederebbero permessi di root).

**docker-compose-dev.yml** — avvia solo il database PostgreSQL per lo sviluppo.

**Comandi essenziali:**
```bash
docker compose -f docker-compose-dev.yml up -d    # avvia il DB
docker compose -f docker-compose-dev.yml down      # ferma il DB (dati conservati)
docker compose -f docker-compose-dev.yml down -v   # ferma e cancella i dati
docker ps                                          # vedi container in esecuzione
docker exec -it scuola-postgres-dev psql -U scuola -d db_materie  # entra nel DB
```

---

## 11. Testing

**Test del Service** (Mockito puro):
```java
@ExtendWith(MockitoExtension.class)  // nessun contesto Spring, solo Mockito
class MateriaServiceTest {
    @Mock MateriaRepository repo;     // repository finto
    @InjectMocks MateriaService service; // service reale con mock iniettato
    
    @Test
    void creaMateria_dovrebbeCreareLaMateria() {
        // Arrange → Act → Assert
        when(repo.save(any())).thenReturn(materia);
        var result = service.creaMateria(dto);
        assertThat(result.nome()).isEqualTo("Matematica");
    }
}
```

**Perché i mock?** Per testare la logica senza toccare il database reale.

---

## 12. Stub WireMock

**Cos'è uno stub?** Una versione finta del tuo microservizio che i compagni usano per sviluppare senza avere il tuo servizio attivo.

**Cartella:** `materie-service-stub/mappings/` — file JSON che descrivono request/response.

**Come usarlo:**
```bash
java -jar wiremock-standalone.jar --port 5003
# I compagni chiamano http://localhost:5003/materie
```

---

## 13. Frase riassuntiva per l'esame

*"Il `materie-service` è un microservizio Spring Boot che gestisce il catalogo delle materie scolastiche. Segue l'architettura a strati: il client chiama `/materie`, il `VersioningFilter` converte il path in `/api/v1/materie`, il `JwtAuthFilter` valida il token JWT emesso da `utenti-service`, poi il `Controller` delega al `Service` che usa il `Repository` (che estende `CrudRepository`) per interagire con PostgreSQL. La documentazione è generata automaticamente da Springdoc su `/swagger-ui/index.html`, i test unitari usano Mockito per testare la logica senza toccare il database, e il progetto gira in Docker."*
