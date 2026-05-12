import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BrowserService } from './shared/services/browser.service';
import { ToastContainer } from './shared/components/toast-container/toast-container';
import { CookieBanner } from './shared/components/cookie-banner/cookie-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer, CookieBanner],
  templateUrl: './app.html'
})
export class App {
  // Initialises document-level side effects: meta description, scroll restore and global key listeners.
  constructor() {
    inject(BrowserService).init();
  }
}
