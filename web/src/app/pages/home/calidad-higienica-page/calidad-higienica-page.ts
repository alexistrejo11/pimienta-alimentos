import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShellComponent } from '../../../shared/public/public-shell';

@Component({
  selector: 'app-calidad-higienica-page',
  imports: [RouterLink, PublicShellComponent],
  templateUrl: './calidad-higienica-page.html',
})
export class CalidadHigienicaPage {}
