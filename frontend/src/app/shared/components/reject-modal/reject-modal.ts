import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-reject-modal',
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './reject-modal.html',
})
export class RejectModal {
  @Input() show = false;
  @Input() processing = false;
  @Output() confirmed = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  reason = '';
  submitted = false;

  onCancel(): void {
    this.reason = '';
    this.submitted = false;
    this.cancelled.emit();
  }

  onConfirm(): void {
    this.submitted = true;
    if (!this.reason.trim()) {
      return;
    }
    const reasonToEmit = this.reason;
    this.reason = '';
    this.submitted = false;
    this.confirmed.emit(reasonToEmit);
  }
}
