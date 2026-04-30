import { Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChildren } from '@angular/core';
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

  @ViewChildren('optionItem') optionItems!: QueryList<ElementRef<HTMLLIElement>>;

  activeIndex = -1;

  onTriggerKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (!this.isOpen) {
        this.toggled.emit();
      }
      this.activeIndex = event.key === 'ArrowUp' ? Math.max(0, this.options.length - 1) : 0;
      setTimeout(() => this.focusOption(this.activeIndex));
    } else if (event.key === 'Escape' && this.isOpen) {
      event.preventDefault();
      this.toggled.emit();
    }
  }

  onOptionKeydown(event: KeyboardEvent, index: number): void {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.activeIndex = (index + 1) % this.options.length;
        this.focusOption(this.activeIndex);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeIndex = (index - 1 + this.options.length) % this.options.length;
        this.focusOption(this.activeIndex);
        break;
      case 'Home':
        event.preventDefault();
        this.activeIndex = 0;
        this.focusOption(this.activeIndex);
        break;
      case 'End':
        event.preventDefault();
        this.activeIndex = this.options.length - 1;
        this.focusOption(this.activeIndex);
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        this.selected.emit(this.options[index].value);
        break;
      case 'Escape':
        event.preventDefault();
        if (this.isOpen) {
          this.toggled.emit();
        }
        break;
    }
  }

  private focusOption(index: number): void {
    const item = this.optionItems?.toArray()[index];
    item?.nativeElement.focus();
  }
}
