import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShellComponent } from '../../../shared/public/public-shell';

@Component({
  selector: 'app-terminos-servicio-page',
  imports: [RouterLink, PublicShellComponent],
  templateUrl: './terminos-servicio-page.html',
})
export class TerminosServicioPage {}
