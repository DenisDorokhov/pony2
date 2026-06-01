import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  private static readonly THEME_LOCAL_STORAGE_KEY = 'pony2.ThemeService.theme';

  private readonly themeSubject = new BehaviorSubject<Theme>(this.loadDefaultTheme());

  constructor() {
    this.applyTheme(this.themeSubject.value);
  }

  get theme(): Theme {
    return this.themeSubject.value;
  }

  set theme(theme: Theme) {
    localStorage.setItem(ThemeService.THEME_LOCAL_STORAGE_KEY, theme);
    this.applyTheme(theme);
    this.themeSubject.next(theme);
  }

  observeTheme(): Observable<Theme> {
    return this.themeSubject.asObservable();
  }

  toggleTheme() {
    this.theme = this.theme === 'dark' ? 'light' : 'dark';
  }

  private loadDefaultTheme(): Theme {
    const appliedTheme = window.document.documentElement.getAttribute('data-bs-theme');
    if (appliedTheme === 'light' || appliedTheme === 'dark') {
      return appliedTheme;
    }
    return 'light';
  }

  private applyTheme(theme: Theme) {
    window.document.documentElement.setAttribute('data-bs-theme', theme);
  }
}
