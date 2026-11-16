import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

interface Annonce {
  id: number;
  texte: string;
  competences: string[];
  creeLe: string;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/annonces';

  texte = '';
  readonly exemples = 'Nous cherchons une personne qui maîtrise Python et SQL.';
  readonly max = 5000;
  readonly annonces = signal<Annonce[]>([]);
  readonly chargement = signal(false);
  readonly envoi = signal(false);
  readonly erreurListe = signal('');
  readonly erreurEnvoi = signal('');
  readonly confirmation = signal('');

  constructor() {
    this.charger();
  }

  charger(): void {
    if (this.chargement()) return;
    this.erreurListe.set('');
    this.chargement.set(true);
    this.http.get<Annonce[]>(this.url).pipe(finalize(() => this.chargement.set(false))).subscribe({
      next: (annonces) => this.annonces.set(annonces),
      error: () => this.erreurListe.set('Impossible de charger les annonces.'),
    });
  }

  analyser(): void {
    const texte = this.texte.trim();
    if (!texte || texte.length > this.max || this.envoi() || this.chargement()) return;
    this.erreurEnvoi.set('');
    this.confirmation.set('');
    this.envoi.set(true);
    this.http.post<Annonce>(this.url, { texte }).pipe(finalize(() => this.envoi.set(false))).subscribe({
      next: (annonce) => {
        this.annonces.update((liste) => [annonce, ...liste]);
        this.texte = '';
        this.confirmation.set('Annonce analysée et enregistrée.');
      },
      error: (err: HttpErrorResponse) => {
        this.erreurEnvoi.set(
          typeof err.error?.message === 'string'
            ? err.error.message
            : 'L’analyse a échoué. Tu peux réessayer sans ressaisir ton texte.',
        );
      },
    });
  }
}
