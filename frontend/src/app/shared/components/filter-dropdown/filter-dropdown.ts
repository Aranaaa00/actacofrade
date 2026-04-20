import { Component, Input, Output, EventEmitter } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

export interface FilterOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-filter-dropdown',
  imports: [LucideAngularModule],
  templateUrl: './filter-dropdown.html',
})
export class FilterDropdown {
  @Input() label = '';
  @Input() options: FilterOption[] = [];
  @Input() isOpen = false;
  @Output() toggled = new EventEmitter<void>();
  @Output() selected = new EventEmitter<string>();
}
