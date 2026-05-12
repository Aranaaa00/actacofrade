import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConsentService } from '../../services/consent.service';

// Anchored notice presented on first visit (and again after the user resets
// their preferences from the cookie policy). It offers the two choices
// required by the spec: enable every optional category, or keep only the
// strictly necessary storage. Anything optional must stay off until the user
// picks "Aceptar todo".
@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink],
  templateUrl: './cookie-banner.html',
})
export class CookieBanner {
  private readonly consent = inject(ConsentService);

  readonly visible = computed(() => !this.consent.hasDecided());

  acceptAll(): void {
    this.consent.acceptAll();
  }

  essentialOnly(): void {
    this.consent.acceptEssentialOnly();
  }
}
