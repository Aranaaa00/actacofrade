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
}
