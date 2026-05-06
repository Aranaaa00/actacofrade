import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { UserResponse } from '../../../models/user.model';
import { ModalA11yDirective } from '../../directives/modal-a11y.directive';

interface RoleOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-edit-user-modal',
  imports: [FormsModule, LucideAngularModule, ModalA11yDirective],
  templateUrl: './edit-user-modal.html',
})
export class EditUserModal implements OnChanges {
  @Input() show = false;
  @Input() processing = false;
  @Input() user: UserResponse | null = null;
  @Input() roles: RoleOption[] = [];
  @Output() confirmed = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  selectedRole = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['user'] || changes['show']) {
      this.selectedRole = this.user?.roles?.[0] ?? '';
    }
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  onConfirm(): void {
    if (!this.selectedRole) {
      return;
    }
    this.confirmed.emit(this.selectedRole);
  }
}
