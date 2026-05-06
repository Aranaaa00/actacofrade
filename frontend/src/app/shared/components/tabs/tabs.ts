import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-tabs',
  templateUrl: './tabs.html',
})
export class Tabs {
  @Input() tabs: string[] = [];
  @Input() activeTab = '';
  @Output() tabChanged = new EventEmitter<string>();

  selectTab(tab: string): void {
    this.tabChanged.emit(tab);
  }

  onKeydown(event: KeyboardEvent, index: number): void {
    const total = this.tabs.length;
    if (total === 0) {
      return;
    }
    let nextIndex: number | null = null;
    if (event.key === 'ArrowRight') {
      nextIndex = (index + 1) % total;
    } else if (event.key === 'ArrowLeft') {
      nextIndex = (index - 1 + total) % total;
    } else if (event.key === 'Home') {
      nextIndex = 0;
    } else if (event.key === 'End') {
      nextIndex = total - 1;
    }
    if (nextIndex === null) {
      return;
    }
    event.preventDefault();
    const target = event.currentTarget as HTMLElement;
    const buttons = target.parentElement?.querySelectorAll<HTMLButtonElement>('button.tab');
    buttons?.item(nextIndex)?.focus();
    this.tabChanged.emit(this.tabs[nextIndex]);
  }
}
