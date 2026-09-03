import { HttpClient } from '@angular/common/http';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isPlatformBrowser } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { fromEvent, finalize } from 'rxjs';

import { BRAND_LOGO_URL } from '../brand';
import { CONTACT_ENDPOINT } from '../contact-endpoint';
import { LANDING_CONTENT, LANDING_NAV } from './landing-content';

@Component({
  selector: 'app-home',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);

  /** Set to false to hide the brand cover and start at Empresa. */
  readonly showBrandCover = true;

  readonly logoUrl = BRAND_LOGO_URL;
  readonly content = LANDING_CONTENT;
  readonly navItems = LANDING_NAV;

  readonly navScrolled = signal(false);
  readonly activeSection = signal('inicio');

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    empresa: [''],
    correo: ['', [Validators.required, Validators.email]],
    mensaje: ['', Validators.required],
  });

  readonly statusMessage = signal<string | null>(null);
  readonly statusType = signal<'success' | 'error' | null>(null);
  readonly isSubmitting = signal(false);

  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.setupScrollNav();
    this.setupRevealObserver();
  }

  scrollToSection(id: string): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const targetId = id === 'inicio' && !this.showBrandCover ? 'empresa' : id;
    document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth' });
  }

  contactPhoneHref(): string {
    return `tel:${this.content.contacto.phone.replace(/\s/g, '')}`;
  }

  navLinkClass(id: string): string {
    const base =
      'landing-nav__link font-headline border-b-2 pb-1 text-sm font-bold tracking-tight md:text-base';
    return this.activeSection() === id ? `${base} landing-nav__link--active` : base;
  }

  onSubmit(): void {
    this.statusMessage.set(null);
    this.statusType.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { nombre, empresa, correo, mensaje } = this.form.getRawValue();
    const payload = {
      nombre: nombre.trim(),
      empresa: empresa.trim() || null,
      correo: correo.trim(),
      mensaje: mensaje.trim(),
    };

    this.isSubmitting.set(true);
    this.http
      .post<unknown>(CONTACT_ENDPOINT, payload, {
        headers: {
          Accept: 'application/json',
        },
      })
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.statusType.set('success');
          this.statusMessage.set(
            'Gracias. Hemos recibido su mensaje; nos pondremos en contacto pronto.',
          );
          this.form.reset();
        },
        error: () => {
          this.statusType.set('error');
          this.statusMessage.set(
            'No se pudo enviar el mensaje. Compruebe su conexión o configure CONTACT_ENDPOINT.',
          );
        },
      });
  }

  clearStatusOnInput(): void {
    if (this.statusMessage()) {
      this.statusMessage.set(null);
      this.statusType.set(null);
    }
  }

  fieldError(field: 'nombre' | 'correo' | 'mensaje'): string | null {
    const c = this.form.controls[field];
    if (!c.touched && !c.dirty) {
      return null;
    }
    if (field === 'correo' && c.hasError('email')) {
      return 'Correo no válido.';
    }
    if (c.hasError('required')) {
      if (field === 'nombre') {
        return 'Ingrese su nombre.';
      }
      if (field === 'correo') {
        return 'Ingrese su correo.';
      }
      return 'Escriba un mensaje.';
    }
    return null;
  }

  controlClass(field: 'nombre' | 'correo' | 'mensaje'): string {
    const base = 'landing-input';
    const textarea = field === 'mensaje' ? ' landing-input--textarea' : '';
    const err = this.fieldError(field) ? ' landing-input--error' : '';
    return `${base}${textarea}${err}`.trim();
  }

  statusAlertClass(): string {
    const t = this.statusType();
    if (t === 'success') {
      return 'mb-5 rounded-lg border border-green-600/25 bg-green-50 px-4 py-3.5 text-[0.9375rem] leading-snug text-green-900';
    }
    if (t === 'error') {
      return 'mb-5 rounded-lg border border-[color-mix(in_srgb,var(--color-error)_25%,transparent)] bg-red-50 px-4 py-3.5 text-[0.9375rem] leading-snug text-red-900';
    }
    return '';
  }

  private setupScrollNav(): void {
    fromEvent(window, 'scroll', { passive: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.navScrolled.set(window.scrollY > 24);
      });
  }

  private setupRevealObserver(): void {
    const root = this.host.nativeElement;
    const sections = Array.from(
      root.querySelectorAll('[data-landing-section]'),
    ) as HTMLElement[];
    const revealBlocks = Array.from(root.querySelectorAll('.landing-reveal')) as HTMLElement[];

    const sectionObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            this.activeSection.set(entry.target.id);
          }
        }
      },
      { rootMargin: '-40% 0px -50% 0px', threshold: 0 },
    );

    for (const section of sections) {
      sectionObserver.observe(section);
    }

    const revealObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            revealObserver.unobserve(entry.target);
          }
        }
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.08 },
    );

    for (const block of revealBlocks) {
      revealObserver.observe(block);
    }

    this.destroyRef.onDestroy(() => {
      sectionObserver.disconnect();
      revealObserver.disconnect();
    });
  }
}
