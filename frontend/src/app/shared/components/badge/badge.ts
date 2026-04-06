import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-badge',
  templateUrl: './badge.html',
})
export class Badge {
  @Input() variant = '';
  @Input() styleClass = '';
}
