import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-legal-page',
  imports: [RouterLink],
  templateUrl: './legal-page.html',
})
export class LegalPage {
  @Input() title = '';
  @Input() updatedAt = '';
}
