import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <a routerLink="/search" class="brand">
          <span class="brand-mark" aria-hidden="true">☀</span><span>Sun Travels</span>
        </a>
        <nav aria-label="Main navigation">
          <a routerLink="/search" routerLinkActive="selected" ariaCurrentWhenActive="page">Room search</a>
          <a routerLink="/contracts" routerLinkActive="selected" ariaCurrentWhenActive="page">Hotel contracts</a>
        </nav>
      </aside>
      <main class="workspace"><router-outlet /></main>
    </div>
  `,
})
export class App {}
