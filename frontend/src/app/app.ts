import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BrowserService } from './shared/services/browser.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html'
})
export class App {
  // Bootstraps document-level side effects (description, scroll, key listeners).
  constructor() {
    inject(BrowserService).init();
  }
}
