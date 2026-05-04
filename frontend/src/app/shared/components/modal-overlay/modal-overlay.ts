import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { APP_ESCAPE_EVENT } from '../../services/browser.service';

// Reusable modal frame: closes on overlay click and on the global escape event.
@Component({
  selector: 'app-modal-overlay',
  imports: [LucideAngularModule],
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

  // Closes the modal when the user releases overlay click on the backdrop.
  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.closed.emit();
    }
  }

  private readonly onEscape = (): void => {
    this.closed.emit();
  };
}
