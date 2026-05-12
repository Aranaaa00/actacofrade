import { Component } from '@angular/core';
import { LegalPage } from '../../../shared/components/legal-page/legal-page';

@Component({
  selector: 'app-privacy',
  imports: [LegalPage],
  templateUrl: './privacy.html',
})
export class Privacy {
  readonly updatedAt = '12 de mayo de 2026';
}
