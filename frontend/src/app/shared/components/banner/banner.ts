import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-banner',
  templateUrl: './banner.html',
})
export class Banner {
  @Input() type = '';
  @Input() role = 'alert';
  @Input() ariaLive: 'assertive' | 'polite' = 'assertive';
  @Input() styleClass = '';
}
