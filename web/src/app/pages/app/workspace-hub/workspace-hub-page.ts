import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ERP_HOME, OPS_HOME } from '../../../core/auth/workspace-area';
import { UserProfileModalComponent } from '../../../shared/ui/user-profile-modal/user-profile-modal';
import { ThemeToggleComponent } from '../../../shared/ui/theme-toggle/theme-toggle';
import { BRAND_LOGO_URL } from '../../home/brand';

@Component({
  selector: 'app-workspace-hub-page',
  imports: [RouterLink, ThemeToggleComponent, UserProfileModalComponent],
  templateUrl: './workspace-hub-page.html',
})
export class WorkspaceHubPageComponent {
  readonly logoUrl = BRAND_LOGO_URL;
  readonly opsHome = OPS_HOME;
  readonly erpHome = ERP_HOME;
  readonly mostrarPerfil = signal(false);
}
