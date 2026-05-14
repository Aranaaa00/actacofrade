import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-verified-badge',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './verified-badge.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifiedBadge {
  @Input() verified: boolean | null | undefined = false;
  @Input() size = 14;
  @Input() label = 'Usuario verificado';
  @Input() styleClass = '';
}
