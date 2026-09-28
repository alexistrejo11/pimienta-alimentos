import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { authTokenStorage } from '../../../core/auth/auth-token.storage';
import { SessionContextService } from '../../../core/auth/session-context.service';

@Component({
  selector: 'app-pending-approval-page',
  imports: [RouterLink],
  templateUrl: './pending-approval-page.html',
})
export class PendingApprovalPageComponent {
  private readonly auth = inject(AuthService);
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);

  readonly loggingOut = signal(false);

  cerrarSesion(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    this.auth
      .logout()
      .pipe(
        finalize(() => {
          authTokenStorage.clear();
          this.session.clear();
          this.loggingOut.set(false);
          void this.router.navigate(['/auth/login']);
        }),
      )
      .subscribe({
        next: () => undefined,
        error: () => undefined,
      });
  }
}
