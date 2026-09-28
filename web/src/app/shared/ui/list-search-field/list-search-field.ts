import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-list-search-field',
  imports: [FormsModule],
  templateUrl: './list-search-field.html',
})
export class ListSearchFieldComponent {
  readonly label = input<string | undefined>(undefined);
  readonly placeholder = input('Buscar…');
  readonly disabled = input(false);
  readonly buttonLabel = input('Buscar');

  /** Texto en el input (borrador); no dispara peticiones por sí solo. */
  readonly draft = model('');

  /** Se emite con el término aplicado al pulsar Enter o Buscar. */
  readonly search = output<string>();

  apply(): void {
    if (this.disabled()) return;
    this.search.emit(this.draft().trim());
  }

  onEnter(event: Event): void {
    event.preventDefault();
    this.apply();
  }
}
