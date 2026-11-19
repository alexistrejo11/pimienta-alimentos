import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { authTokenStorage } from '../../../core/auth/auth-token.storage';
import { sessionAuthPolicy } from '../../../core/auth/session-auth.policy';
import { SessionContextService } from '../../../core/auth/session-context.service';
import { coerceWorkspaceUrl } from '../../../core/auth/workspace-area';
import { displayApiErrorMessage, parseApiError } from '../../../core/http/parse-api-error';
import { BRAND_LOGO_URL } from '../../home/brand';

@Component({
  selector: 'app-servicio-no-disponible',
  imports: [RouterLink],
  templateUrl: './servicio-no-disponible.html',
})
export class ServicioNoDisponiblePage {
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly logoUrl = BRAND_LOGO_URL;
  readonly retrying = signal(false);
  readonly errorMessage = signal<string | null>(null);

  retry(): void {
    if (this.retrying()) {
      return;
    }
    this.retrying.set(true);
    this.errorMessage.set(null);
    this.session
      .ensureLoaded()
      .pipe(finalize(() => this.retrying.set(false)))
      .subscribe({
        next: (context) => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          const target = coerceWorkspaceUrl(returnUrl, {
            roles: context.roles,
            accountStatus: context.accountStatus,
          });
          void this.router.navigateByUrl(target || '/app');
        },
        error: (err: unknown) => {
          if (sessionAuthPolicy.requiresSessionClear(err)) {
            authTokenStorage.clear();
            this.session.clear();
            void this.router.navigate(['/auth/login']);
            return;
          }
          this.errorMessage.set(displayApiErrorMessage(parseApiError(err)));
        },
      });
  }
}
