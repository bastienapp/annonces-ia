"""Teste le vrai parcours et ajoute une annonce fictive à PostgreSQL."""

import json
import sys
from urllib.request import Request, urlopen

base = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
endpoint = base.rstrip("/") + "/api/annonces"
texte = "Nous cherchons une personne qui maîtrise Python et SQL."
req = Request(endpoint, data=json.dumps({"texte": texte}).encode("utf-8"),
              headers={"Content-Type": "application/json"})

with urlopen(req, timeout=45) as response:
    assert response.status == 201, response.status
    annonce = json.load(response)

assert annonce["texte"] == texte, annonce
assert set(annonce["competences"]) == {"Python", "SQL"}, annonce
with urlopen(endpoint, timeout=10) as response:
    historique = json.load(response)
assert any(a == annonce for a in historique), (annonce, historique)
print(f"OK — annonce nº {annonce['id']} analysée par n8n et retrouvée dans l’historique.")
