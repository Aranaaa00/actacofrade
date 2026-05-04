import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-banner',
  templateUrl: './banner.html',
})
export class Banner {
  @Input() type = '';
  @Input() role: string | null = null;
  @Input() ariaLive: 'assertive' | 'polite' | null = null;
  @Input() styleClass = '';

  // announce errors assertively, success and info announcements should be polite (WCAG 2.1 AA SC 4.1.3)
  get effectiveRole(): string {
    if (this.role) return this.role;
    return this.isCritical() ? 'alert' : 'status';
  }

  get effectiveAriaLive(): 'assertive' | 'polite' {
    if (this.ariaLive) return this.ariaLive;
    return this.isCritical() ? 'assertive' : 'polite';
  }

  private isCritical(): boolean {
    return this.type === 'critical' || this.type === 'danger' || this.type === 'error';
  }
}
