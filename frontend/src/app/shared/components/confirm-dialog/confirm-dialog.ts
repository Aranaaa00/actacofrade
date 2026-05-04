import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ModalOverlay } from '../modal-overlay/modal-overlay';

@Component({
  selector: 'app-confirm-dialog',
  imports: [ModalOverlay],
  templateUrl: './confirm-dialog.html',
})
export class ConfirmDialog {
  @Input() titleId = 'confirm-dialog-title';
  @Input() title = 'Confirmar acción';
  @Input() message = '¿Seguro que deseas continuar?';
  @Input() confirmLabel = 'Eliminar';
  @Input() cancelLabel = 'Cancelar';
  @Input() variant: 'danger' | 'primary' = 'danger';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
