import { Component } from '@angular/core';
import { LegalPage } from '../../../shared/components/legal-page/legal-page';

@Component({
  selector: 'app-terms',
  imports: [LegalPage],
  templateUrl: './terms.html',
})
export class Terms {
  readonly updatedAt = '12 de mayo de 2026';
}
