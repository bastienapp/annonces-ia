package fr.hackathon.annonces;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
class N8nClient {
    private final RestClient client;
    private final URI webhook;

    N8nClient(@Value("${n8n.webhook-url}") String url,
              @Value("${n8n.webhook-secret}") String secret) {
        webhook = URI.create(url);
        if (webhook.getHost() == null
                || !("https".equalsIgnoreCase(webhook.getScheme())
                || "http".equalsIgnoreCase(webhook.getScheme()))) {
            throw new IllegalArgumentException("N8N_WEBHOOK_URL doit être une URL HTTP ou HTTPS absolue");
        }

        // Le client conserve les contrôles TLS standards de Java.
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
        factory.setReadTimeout(Duration.ofSeconds(30));

        var builder = RestClient.builder().requestFactory(factory);
        if (!secret.isBlank()) {
            builder.defaultHeader("X-Workshop-Key", secret);
        }
        client = builder.build();
    }

    List<String> analyser(String texte) {
        final ReponseN8n reponse;
        try {
            reponse = client.post().uri(webhook)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("texte", texte))
                    .retrieve().body(ReponseN8n.class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Impossible de joindre n8n ou délai dépassé.", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "n8n a renvoyé une erreur ou une réponse illisible.", e);
        }

        if (reponse == null || reponse.competences() == null
                || reponse.competences().stream().anyMatch(c -> !(c instanceof String s)
                        || s.isBlank() || s.strip().length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Format inattendu : n8n doit renvoyer un tableau de compétences.");
        }
        return reponse.competences().stream().map(c -> ((String) c).strip()).distinct().toList();
    }

    private record ReponseN8n(List<?> competences) {}
}
