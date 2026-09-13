import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BRAND_LOGO_URL } from '../../pages/home/brand';
import { ThemeToggleComponent } from '../ui/theme-toggle/theme-toggle';

@Component({
  selector: 'app-public-shell',
  imports: [RouterLink, ThemeToggleComponent],
  templateUrl: './public-shell.html',
})
export class PublicShellComponent {
  readonly logoUrl = BRAND_LOGO_URL;
}
