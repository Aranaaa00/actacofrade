import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-modal-overlay',
  templateUrl: './modal-overlay.html',
})
export class ModalOverlay {
  @Input() titleId = '';
  @Input() modalClass = '';
  @Input() headerClass = 'modal__header';
  @Output() closed = new EventEmitter<void>();

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.closed.emit();
    }
  }
}
