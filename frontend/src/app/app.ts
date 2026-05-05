import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BrowserService } from './shared/services/browser.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html'
})
export class App {
  // Initialises document-level side effects: meta description, scroll restore and global key listeners.
  constructor() {
    inject(BrowserService).init();
  }
}
