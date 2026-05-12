import { Component, inject } from '@angular/core';
import { LegalPage } from '../../../shared/components/legal-page/legal-page';
import { ConsentService } from '../../../shared/services/consent.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-cookies',
  imports: [LegalPage],
  templateUrl: './cookies.html',
})
export class Cookies {
  private readonly consent = inject(ConsentService);
  private readonly toast = inject(ToastService);

  readonly updatedAt = '12 de mayo de 2026';
  readonly status = this.consent.status;
  readonly categories = this.consent.categories;

  reviewPreferences(): void {
    this.consent.reset();
    this.toast.info('Hemos vuelto a abrir el aviso de cookies.');
  }
}
