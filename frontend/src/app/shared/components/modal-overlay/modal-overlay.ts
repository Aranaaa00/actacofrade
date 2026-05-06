import { DOCUMENT } from '@angular/common';
import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { APP_ESCAPE_EVENT } from '../../services/browser.service';
import { ModalA11yDirective } from '../../directives/modal-a11y.directive';

@Component({
  selector: 'app-modal-overlay',
  imports: [LucideAngularModule, ModalA11yDirective],
  templateUrl: './modal-overlay.html',
})
export class ModalOverlay implements OnInit {
  @Input() titleId = '';
  @Input() modalClass = '';
  @Input() headerClass = 'modal__header';
  @Output() closed = new EventEmitter<void>();

  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.document.addEventListener(APP_ESCAPE_EVENT, this.onEscape);
    this.destroyRef.onDestroy(() => {
      this.document.removeEventListener(APP_ESCAPE_EVENT, this.onEscape);
    });
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.closed.emit();
    }
  }

  private readonly onEscape = (): void => {
    this.closed.emit();
  };
}

