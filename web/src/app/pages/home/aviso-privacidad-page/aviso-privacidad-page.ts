import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShellComponent } from '../../../shared/public/public-shell';

@Component({
  selector: 'app-aviso-privacidad-page',
  imports: [RouterLink, PublicShellComponent],
  templateUrl: './aviso-privacidad-page.html',
})
export class AvisoPrivacidadPage {}
