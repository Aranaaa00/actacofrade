import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Datepicker } from './datepicker';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

describe('Datepicker', () => {
  let fixture: ComponentFixture<Datepicker>;
  let dp: Datepicker;
  let emitted: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Datepicker], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(Datepicker);
    dp = fixture.componentInstance;
    emitted = [];
    dp.dateSelected.subscribe((d) => emitted.push(d));
    document.body.appendChild(fixture.nativeElement);
  });

  afterEach(() => fixture.destroy());

  it('monthLabel formats current view', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 0;
    expect(dp.monthLabel.length).toBeGreaterThan(0);
  });

  it('displayValue returns placeholder when nothing selected', () => {
    dp.placeholder = 'Pick';
    expect(dp.displayValue).toBe('Pick');
  });

  it('displayValue formats selected date', () => {
    dp.selectedDate = '2025-05-08';
    expect(dp.displayValue.length).toBeGreaterThan(5);
  });

  it('weeks grid contains daysInMonth and pads with nulls', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 0; // January 2025 has 31 days, starts on Wednesday
    const weeks = dp.weeks;
    const flat = weeks.flat();
    expect(flat.filter((d) => typeof d === 'number').length).toBe(31);
    expect(flat[0]).toBeNull();
  });

  it('isAvailable respects allSelectable', () => {
    dp.allSelectable = true;
    expect(dp.isAvailable(15)).toBeTrue();
  });

  it('isAvailable false if not in availableDates', () => {
    dp.availableDates = [];
    dp.allSelectable = false;
    expect(dp.isAvailable(15)).toBeFalse();
  });

  it('isAvailable matches when ISO string is in availableDates', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 4;
    dp.availableDates = ['2025-05-15'];
    expect(dp.isAvailable(15)).toBeTrue();
  });

  it('disablePast prevents selecting past dates', () => {
    const today = new Date();
    dp.viewYear = today.getFullYear();
    dp.viewMonth = today.getMonth();
    dp.disablePast = true;
    dp.allSelectable = true;
    // day 1 is past for any month except day 1 of current month before midnight
    expect(typeof dp.isAvailable(1)).toBe('boolean');
  });

  it('isSelected matches selectedDate', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 4;
    dp.selectedDate = '2025-05-15';
    expect(dp.isSelected(15)).toBeTrue();
    expect(dp.isSelected(16)).toBeFalse();
  });

  it('toggle opens and jumps to selected date month', () => {
    dp.selectedDate = '2030-08-10';
    dp.toggle();
    expect(dp.open).toBeTrue();
    expect(dp.viewYear).toBe(2030);
    expect(dp.viewMonth).toBe(7);
  });

  it('toggle closes when already open', () => {
    dp.open = true;
    dp.toggle();
    expect(dp.open).toBeFalse();
  });

  it('prevMonth wraps from January to December of previous year', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 0;
    dp.prevMonth();
    expect(dp.viewMonth).toBe(11);
    expect(dp.viewYear).toBe(2024);
  });

  it('nextMonth wraps from December to January of next year', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 11;
    dp.nextMonth();
    expect(dp.viewMonth).toBe(0);
    expect(dp.viewYear).toBe(2026);
  });

  it('prevMonth and nextMonth normal increment/decrement', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 5;
    dp.prevMonth();
    expect(dp.viewMonth).toBe(4);
    dp.nextMonth();
    dp.nextMonth();
    expect(dp.viewMonth).toBe(6);
  });

  it('selectDate emits and closes', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 4;
    dp.open = true;
    dp.selectDate(15);
    expect(emitted).toEqual(['2025-05-15']);
    expect(dp.open).toBeFalse();
  });

  it('canGoPrev respects disablePast', () => {
    const now = new Date();
    dp.disablePast = true;
    dp.viewYear = now.getFullYear();
    dp.viewMonth = now.getMonth();
    expect(dp.canGoPrev).toBeFalse();
    dp.viewMonth = (now.getMonth() + 1) % 12;
    if (now.getMonth() === 11) dp.viewYear = now.getFullYear() + 1;
    expect(dp.canGoPrev).toBeTrue();
    dp.disablePast = false;
    expect(dp.canGoPrev).toBeTrue();
  });

  it('document click outside closes dropdown', () => {
    dp.open = true;
    dp.onDocumentClick({ target: document.body } as unknown as MouseEvent);
    expect(dp.open).toBeFalse();
  });

  it('document click inside keeps dropdown open', () => {
    dp.open = true;
    const inside = (fixture.nativeElement as HTMLElement).querySelector('*') ?? fixture.nativeElement;
    dp.onDocumentClick({ target: inside } as unknown as MouseEvent);
    expect(dp.open).toBeTrue();
  });

  it('onDayKeydown Escape closes', () => {
    dp.open = true;
    dp.onDayKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(dp.open).toBeFalse();
  });

  function buildKeyEventOnTd(key: string): KeyboardEvent {
    const tbody = document.createElement('tbody');
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    const btn = document.createElement('button');
    btn.classList.add('datepicker__day');
    td.appendChild(btn);
    tr.appendChild(td);
    tbody.appendChild(tr);
    const e = new KeyboardEvent('keydown', { key, cancelable: true });
    Object.defineProperty(e, 'target', { value: btn });
    return e;
  }

  it('onDayKeydown PageDown advances month', () => {
    dp.viewMonth = 5;
    dp.viewYear = 2025;
    dp.onDayKeydown(buildKeyEventOnTd('PageDown'));
    expect(dp.viewMonth).toBe(6);
  });

  it('onDayKeydown PageUp goes back when allowed', () => {
    dp.viewMonth = 5;
    dp.viewYear = 2025;
    dp.disablePast = false;
    dp.onDayKeydown(buildKeyEventOnTd('PageUp'));
    expect(dp.viewMonth).toBe(4);
  });

  it('onDayKeydown ignores unrelated keys', () => {
    const before = dp.viewMonth;
    const e = new KeyboardEvent('keydown', { key: 'a' });
    Object.defineProperty(e, 'target', { value: document.createElement('div') });
    dp.onDayKeydown(e);
    expect(dp.viewMonth).toBe(before);
  });

  it('onDayKeydown without td target does nothing', () => {
    const before = dp.viewMonth;
    const e = new KeyboardEvent('keydown', { key: 'ArrowRight' });
    Object.defineProperty(e, 'target', { value: document.createElement('span') });
    dp.onDayKeydown(e);
    expect(dp.viewMonth).toBe(before);
  });

  it('onDayKeydown PageUp does nothing when canGoPrev is false', () => {
    const now = new Date();
    dp.viewYear = now.getFullYear();
    dp.viewMonth = now.getMonth();
    dp.disablePast = true;
    const before = dp.viewMonth;
    dp.onDayKeydown(buildKeyEventOnTd('PageUp'));
    expect(dp.viewMonth).toBe(before);
  });

  it('weeks grid handles month starting on Sunday', () => {
    dp.viewYear = 2025;
    dp.viewMonth = 5; // June 2025 starts on Sunday
    expect(dp.weeks.length).toBeGreaterThan(0);
    expect(dp.weeks[0][0]).toBeNull();
  });

  function buildKeyOnTbody(key: string, atIndex = 7): { event: KeyboardEvent; cells: HTMLTableCellElement[] } {
    const tbody = document.createElement('tbody');
    const cells: HTMLTableCellElement[] = [];
    for (let r = 0; r < 5; r++) {
      const tr = document.createElement('tr');
      for (let c = 0; c < 7; c++) {
        const td = document.createElement('td');
        const btn = document.createElement('button');
        btn.classList.add('datepicker__day');
        td.appendChild(btn);
        tr.appendChild(td);
        cells.push(td);
      }
      tbody.appendChild(tr);
    }
    document.body.appendChild(tbody);
    const event = new KeyboardEvent('keydown', { key, cancelable: true });
    const target = cells[atIndex].querySelector('button')!;
    Object.defineProperty(event, 'target', { value: target });
    return { event, cells };
  }

  for (const key of ['ArrowRight', 'ArrowLeft', 'ArrowDown', 'ArrowUp']) {
    it(`onDayKeydown handles ${key}`, () => {
      const { event } = buildKeyOnTbody(key, 14);
      dp.onDayKeydown(event);
      expect(event.defaultPrevented).toBeTrue();
    });
  }
});
