# Annonces IA

Exemple minimal en **Angular 21**, **Spring Boot 4 / Java 21** et **PostgreSQL 17** pour le hackathon. On colle le texte d’une offre, puis le modèle extrait les compétences explicitement demandées. L’application conserve l’annonce et le résultat et les affiche dans un historique.

Le navigateur appelle `/api/annonces` sur la même origine que l’interface. Spring Boot envoie `{"texte":"…"}` au webhook n8n. Celui-ci répond avec `{"competences":["Python","SQL"]}`. Le backend valide le format, enregistre le texte et les compétences dans PostgreSQL, puis renvoie l’annonce à Angular. Une liste vide est valide. En cas d’erreur n8n, aucune annonce n’est créée.

## Démarrer

Prérequis : Docker avec Compose v2. n8n est déjà hébergé séparément.

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps
```

Ouvre <http://localhost:8080>. Dans **Coder**, ouvre le port 8080 via le panneau des ports, avec une visibilité privée. Si 8080 est occupé, change `APP_PORT` dans `.env`.

Dans `.env`, `N8N_WEBHOOK_URL` désigne l’URL **de production** `/webhook/…` d’un workflow publié. L’exemple pointe sur `https://n8n.hackeuse.fr/webhook/analyse-offre`. Si **Header Auth** est activé dans n8n, configure le nom d’en-tête `X-Workshop-Key` dans n8n et sa valeur dans `N8N_WEBHOOK_SECRET`. La clé OpenAI reste dans n8n. Laisse `N8N_WEBHOOK_SECRET` vide uniquement pour un essai sans authentification.

L’appel HTTPS à n8n part de **Spring Boot**, pas du navigateur. Pas de `--ssl-revoke-best-effort`, de désactivation TLS ni de réglage CORS dans ce projet.

## Vérifier le parcours réel

Une fois l’application démarrée, sélectionne **Utiliser un exemple**, puis **Analyser et enregistrer**. L’annonce doit apparaître avec les étiquettes Python et SQL. Recharge la page pour vérifier qu’elle est relue depuis PostgreSQL.

Tu peux aussi lancer `python3 scripts/test-parcours.py`. Ce test fait **un appel réel à n8n**, enregistre une annonce fictive, puis la relit via l’API. Si tu as changé de port, passe l’URL locale au script, par exemple `python3 scripts/test-parcours.py http://localhost:8081`.

Le volume PostgreSQL persiste après `docker compose down`. Évite `down -v` si tu veux conserver les annonces.

## API

| Méthode | Route | Résultat |
|---|---|---|
| POST | `/api/annonces` | `201` avec l’annonce et ses compétences, `400` si le texte est invalide ou `502` si n8n échoue |
| GET | `/api/annonces` | Liste des annonces, la plus récente en premier |

```bash
curl --fail-with-body -sS http://localhost:8080/api/annonces \
  -H 'Content-Type: application/json' \
  --data-raw '{"texte":"Nous cherchons une personne qui maîtrise Python et SQL."}'
```

## Structure et tests

- `frontend/` contient la page Angular et le proxy Nginx qui relaie `/api/`.
- `backend/` contient les routes, le client n8n et la persistance JPA. Pour ce projet pédagogique, Hibernate crée le schéma au démarrage et applique les modifications simples.
- `cd backend && mvn verify` lance les tests Spring avec une base H2 et un faux webhook local. Le build Docker les exécute également.
- Pour tester le même parcours avec **PostgreSQL**, utilise `scripts/test-parcours.py` après le démarrage de la stack Compose.

L’interface et l’API ne gèrent pas de comptes utilisateurs. N’expose pas l’application publiquement avec un webhook payant derrière elle.
