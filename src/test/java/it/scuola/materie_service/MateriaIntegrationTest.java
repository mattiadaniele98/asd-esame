package it.scuola.materie_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.scuola.materie_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test di integrazione per MateriaController.
 *
 * Differenza rispetto ai test unitari:
 * - @SpringBootTest avvia il contesto Spring completo (controller + service + repository + DB)
 * - MockMvc viene costruito manualmente dal WebApplicationContext
 * - Si usano dati reali nel database (ogni test è isolato con @Transactional)
 *
 * L'unica cosa "finta" è il JwtService (@MockitoBean):
 * serve per bypassare la validazione del token senza avere utenti-service attivo.
 */
@SpringBootTest
@ActiveProfiles("sviluppo")
@Transactional
class MateriaIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // Creato direttamente invece di iniettarlo da Spring (evita problemi con @MockitoBean)
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * @MockBean sostituisce il JwtService reale con un mock di Mockito.
     * Il JwtAuthFilter chiamerà questo mock invece del servizio reale,
     * permettendoci di simulare token validi senza firme crittografiche.
     */
    @MockitoBean
    private JwtService jwtService;

    private static final String TOKEN = "Bearer token-di-test";
    private static final String URL_MATERIE = "/api/v1/materie";

    @BeforeEach
    void setUp() {
        // Costruisce MockMvc dal contesto Spring con la security chain attiva
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Per ogni test: il token è valido, l'utente ha ruolo SEGRETERIA
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractSubject(any())).thenReturn("test@scuola.it");
        when(jwtService.extractRole(any())).thenReturn("SEGRETERIA");
    }

    // ── GET /materie ──────────────────────────────────────────────────────────

    @Test
    void trovaTutte_senzaToken_ritorna401() throws Exception {
        // Nessun header Authorization → il filtro JWT non autentica → 401
        mockMvc.perform(get(URL_MATERIE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trovaTutte_conToken_ritorna200() throws Exception {
        mockMvc.perform(get(URL_MATERIE)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── POST /materie ─────────────────────────────────────────────────────────

    @Test
    void creaMateria_valida_ritorna201() throws Exception {
        String body = """
                {
                  "nome": "Matematica",
                  "codice": "MAT01",
                  "descrizione": "Matematica di base",
                  "oreSettimanali": 5,
                  "tipoMateria": "TEORICA"
                }
                """;

        mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Matematica"))
                .andExpect(jsonPath("$.codice").value("MAT01"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void creaMateria_campiObbligatoriMancanti_ritorna400() throws Exception {
        // Body vuoto → @Valid nel controller rileva @NotBlank violato → 400
        mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaMateria_codiceDuplicato_ritorna409() throws Exception {
        String prima = """
                {
                  "nome": "Fisica",
                  "codice": "FIS01",
                  "oreSettimanali": 3,
                  "tipoMateria": "TEORICA"
                }
                """;

        // Prima creazione → successo
        mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prima))
                .andExpect(status().isCreated());

        String seconda = """
                {
                  "nome": "Fisica Avanzata",
                  "codice": "FIS01",
                  "oreSettimanali": 4,
                  "tipoMateria": "LABORATORIO"
                }
                """;

        // Seconda creazione con stesso codice → 409 Conflict
        mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seconda))
                .andExpect(status().isConflict());
    }

    // ── GET /materie/{id} ─────────────────────────────────────────────────────

    @Test
    void trovaPerID_esistente_ritorna200() throws Exception {
        // Crea una materia e recupera l'ID dalla risposta
        String body = """
                {
                  "nome": "Chimica",
                  "codice": "CHI01",
                  "oreSettimanali": 3,
                  "tipoMateria": "LABORATORIO"
                }
                """;

        String risposta = mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(risposta).get("id").asText();

        // GET con l'ID appena creato → 200
        mockMvc.perform(get(URL_MATERIE + "/" + id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Chimica"));
    }

    @Test
    void trovaPerID_nonEsistente_ritorna404() throws Exception {
        mockMvc.perform(get(URL_MATERIE + "/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    // ── PUT /materie/{id} ─────────────────────────────────────────────────────

    @Test
    void aggiornaMateria_esistente_ritorna200() throws Exception {
        // Crea
        String create = """
                {
                  "nome": "Storia",
                  "codice": "STO01",
                  "oreSettimanali": 2,
                  "tipoMateria": "TEORICA"
                }
                """;

        String risposta = mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(risposta).get("id").asText();

        // Aggiorna solo il nome e le ore (gli altri campi restano invariati)
        String update = """
                {
                  "nome": "Storia Moderna",
                  "oreSettimanali": 3
                }
                """;

        mockMvc.perform(put(URL_MATERIE + "/" + id)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Storia Moderna"))
                .andExpect(jsonPath("$.oreSettimanali").value(3));
    }

    @Test
    void aggiornaMateria_nonEsistente_ritorna404() throws Exception {
        String update = """
                { "nome": "Qualcosa" }
                """;

        mockMvc.perform(put(URL_MATERIE + "/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /materie/{id} ──────────────────────────────────────────────────

    @Test
    void eliminaMateria_esistente_ritorna204() throws Exception {
        String body = """
                {
                  "nome": "Educazione Fisica",
                  "codice": "EDF01",
                  "oreSettimanali": 2,
                  "tipoMateria": "PRATICA"
                }
                """;

        String risposta = mockMvc.perform(post(URL_MATERIE)
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(risposta).get("id").asText();

        mockMvc.perform(delete(URL_MATERIE + "/" + id)
                        .header("Authorization", TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminaMateria_nonEsistente_ritorna404() throws Exception {
        mockMvc.perform(delete(URL_MATERIE + "/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }
}
