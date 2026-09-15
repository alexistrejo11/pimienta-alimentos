import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';

/** Legacy `/app/pos/cortes` → turnos historial tab. */
@Component({ standalone: true, template: '' })
export class CortesRedirectComponent implements OnInit {
  private readonly router = inject(Router);

  ngOnInit(): void {
    void this.router.navigate(['/app/pos/turnos'], {
      queryParams: { tab: 'history' },
      replaceUrl: true,
    });
  }
}
