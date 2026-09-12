import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'pimienta-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);

  readonly isDark = signal(false);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.isDark.set(document.documentElement.classList.contains('dark'));
    }
  }

  toggle(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  setTheme(mode: ThemeMode): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const dark = mode === 'dark';
    document.documentElement.classList.toggle('dark', dark);
    localStorage.setItem(STORAGE_KEY, mode);
    this.isDark.set(dark);
  }
}
