import { Component, Input, Output, EventEmitter, HostListener, ElementRef, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-datepicker',
  imports: [LucideAngularModule],
  templateUrl: './datepicker.html',
})
export class Datepicker {
  private readonly el = inject(ElementRef);

  @Input() availableDates: string[] = [];
  @Input() selectedDate = '';
  @Input() placeholder = 'Fecha';
  @Output() dateSelected = new EventEmitter<string>();

  open = false;
  viewYear = new Date().getFullYear();
  viewMonth = new Date().getMonth();

  readonly weekDays = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

  get monthLabel(): string {
    const date = new Date(this.viewYear, this.viewMonth);
    return date.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
  }

  get displayValue(): string {
    let result = this.placeholder;
    if (this.selectedDate) {
      const d = new Date(this.selectedDate + 'T00:00:00');
      result = d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }).toUpperCase();
    }
    return result;
  }

  get weeks(): (number | null)[][] {
    const firstDay = new Date(this.viewYear, this.viewMonth, 1);
    const daysInMonth = new Date(this.viewYear, this.viewMonth + 1, 0).getDate();
    let startDay = firstDay.getDay() - 1;
    if (startDay < 0) { startDay = 6; }

    const result: (number | null)[][] = [];
    let week: (number | null)[] = new Array(startDay).fill(null);

    for (let day = 1; day <= daysInMonth; day++) {
      week.push(day);
      if (week.length === 7) {
        result.push(week);
        week = [];
      }
    }
    if (week.length > 0) {
      while (week.length < 7) { week.push(null); }
      result.push(week);
    }
    return result;
  }

  isAvailable(day: number): boolean {
    return this.availableDates.includes(this.toIso(day));
  }

  isSelected(day: number): boolean {
    return this.selectedDate === this.toIso(day);
  }

  toggle(): void {
    this.open = !this.open;
    if (this.open && this.selectedDate) {
      const d = new Date(this.selectedDate + 'T00:00:00');
      this.viewYear = d.getFullYear();
      this.viewMonth = d.getMonth();
    }
  }

  prevMonth(): void {
    if (this.viewMonth === 0) {
      this.viewMonth = 11;
      this.viewYear--;
    } else {
      this.viewMonth--;
    }
  }

  nextMonth(): void {
    if (this.viewMonth === 11) {
      this.viewMonth = 0;
      this.viewYear++;
    } else {
      this.viewMonth++;
    }
  }

  selectDate(day: number): void {
    this.dateSelected.emit(this.toIso(day));
    this.open = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target)) {
      this.open = false;
    }
  }

  private toIso(day: number): string {
    const m = String(this.viewMonth + 1).padStart(2, '0');
    const d = String(day).padStart(2, '0');
    return `${this.viewYear}-${m}-${d}`;
  }
}
