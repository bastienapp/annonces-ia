package fr.hackathon.annonces;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:annonces;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "n8n.webhook-secret=test-only"
})
class AnnonceIntegrationTest {
    private static final AtomicReference<String> REPONSE = new AtomicReference<>();
    private static final AtomicReference<String> CORPS_RECU = new AtomicReference<>();
    private static final AtomicReference<String> ENTETE_RECU = new AtomicReference<>();
    private static final AtomicInteger STATUT_AMONT = new AtomicInteger();
    private static final AtomicInteger APPELS = new AtomicInteger();
    private static final HttpServer WEBHOOK = demarrerWebhook();
    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort int port;
    @Autowired AnnonceRepository repository;

    @DynamicPropertySource
    static void webhook(DynamicPropertyRegistry props) {
        props.add("n8n.webhook-url",
                () -> "http://127.0.0.1:" + WEBHOOK.getAddress().getPort() + "/analyse-offre");
    }

    @BeforeEach
    void reinitialiser() {
        repository.deleteAll();
        CORPS_RECU.set(null);
        ENTETE_RECU.set(null);
        APPELS.set(0);
        STATUT_AMONT.set(200);
        REPONSE.set("{\"competences\":[\"Python\",\"SQL\"]}");
    }

    @AfterAll
    static void arreterWebhook() {
        WEBHOOK.stop(0);
    }

    @Test
    void analyseEnregistreEtRelitUneAnnonce() throws Exception {
        var resultat = creer("{\"texte\":\"Python et SQL demandes.\"}");
        assertThat(resultat.statusCode()).isEqualTo(201);
        assertThat(resultat.body()).contains("Python et SQL demandes.", "\"competences\":[\"Python\",\"SQL\"]");
        assertThat(CORPS_RECU.get()).isEqualTo("{\"texte\":\"Python et SQL demandes.\"}");
        assertThat(ENTETE_RECU.get()).isEqualTo("test-only");
        assertThat(repository.count()).isEqualTo(1);

        var liste = http.send(HttpRequest.newBuilder(url()).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(liste.statusCode()).isEqualTo(200);
        assertThat(liste.body()).isEqualTo("[" + resultat.body() + "]");
        assertThat(APPELS.get()).isEqualTo(1);
    }

    @Test
    void accepteUneListeVide() throws Exception {
        REPONSE.set("{\"competences\":[]}");
        var resultat = creer("{\"texte\":\"Poste a pourvoir lundi.\"}");
        assertThat(resultat.statusCode()).isEqualTo(201);
        assertThat(resultat.body()).contains("\"competences\":[]");
    }

    @Test
    void refuseUnTexteVideSansAppelerN8n() throws Exception {
        assertThat(creer("{\"texte\":\"  \"}").statusCode()).isEqualTo(400);
        assertThat(APPELS.get()).isZero();
        assertThat(repository.count()).isZero();
    }

    @Test
    void refuseUneReponseAuMauvaisFormatSansEnregistrer() throws Exception {
        REPONSE.set("{\"competences\":[42]}");
        assertThat(creer("{\"texte\":\"Python.\"}").statusCode()).isEqualTo(502);
        assertThat(repository.count()).isZero();
    }

    @Test
    void neCreePasDAnnonceSiN8nEchoue() throws Exception {
        STATUT_AMONT.set(503);
        assertThat(creer("{\"texte\":\"Python.\"}").statusCode()).isEqualTo(502);
        assertThat(repository.count()).isZero();
    }

    private URI url() {
        return URI.create("http://127.0.0.1:" + port + "/api/annonces");
    }

    private HttpResponse<String> creer(String corps) throws Exception {
        return http.send(HttpRequest.newBuilder(url())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corps)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer demarrerWebhook() {
        try {
            var serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            serveur.createContext("/analyse-offre", e -> {
                try (e) {
                    APPELS.incrementAndGet();
                    CORPS_RECU.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    ENTETE_RECU.set(e.getRequestHeaders().getFirst("X-Workshop-Key"));
                    byte[] corps = REPONSE.get().getBytes(StandardCharsets.UTF_8);
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(STATUT_AMONT.get(), corps.length);
                    e.getResponseBody().write(corps);
                }
            });
            serveur.start();
            return serveur;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
