import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ThemeToggleComponent } from '../../ui/theme-toggle/theme-toggle';

@Component({
  selector: 'app-workspace-footer',
  imports: [RouterLink, ThemeToggleComponent],
  templateUrl: './workspace-footer.html',
})
export class WorkspaceFooterComponent {}
