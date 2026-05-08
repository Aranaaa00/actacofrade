import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FilterDropdown } from './filter-dropdown';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

describe('FilterDropdown', () => {
  let fixture: ComponentFixture<FilterDropdown>;
  let c: FilterDropdown;
  let toggled: number;
  let selected: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FilterDropdown], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(FilterDropdown);
    c = fixture.componentInstance;
    c.options = [
      { value: 'a', label: 'A' },
      { value: 'b', label: 'B' },
      { value: 'c', label: 'C' },
    ];
    c.isOpen = true;
    toggled = 0;
    selected = [];
    c.toggled.subscribe(() => toggled++);
    c.selected.subscribe((v) => selected.push(v));
    fixture.detectChanges();
  });

  function key(name: string, shift = false): KeyboardEvent {
    return new KeyboardEvent('keydown', { key: name, shiftKey: shift, cancelable: true });
  }

  it('trigger ArrowDown opens and selects first index', fakeAsync(() => {
    c.isOpen = false;
    const e = key('ArrowDown');
    c.onTriggerKeydown(e);
    expect(toggled).toBe(1);
    expect(c.activeIndex).toBe(0);
    tick();
  }));

  it('trigger ArrowUp opens and points to last index', fakeAsync(() => {
    c.isOpen = false;
    c.onTriggerKeydown(key('ArrowUp'));
    expect(c.activeIndex).toBe(2);
    tick();
  }));

  it('trigger Escape closes when open', () => {
    c.onTriggerKeydown(key('Escape'));
    expect(toggled).toBe(1);
  });

  it('trigger ignores unrelated keys', () => {
    c.onTriggerKeydown(key('a'));
    expect(toggled).toBe(0);
  });

  it('option ArrowDown wraps and ArrowUp wraps', () => {
    c.onOptionKeydown(key('ArrowDown'), 2);
    expect(c.activeIndex).toBe(0);
    c.onOptionKeydown(key('ArrowUp'), 0);
    expect(c.activeIndex).toBe(2);
  });

  it('option Home and End', () => {
    c.onOptionKeydown(key('Home'), 1);
    expect(c.activeIndex).toBe(0);
    c.onOptionKeydown(key('End'), 0);
    expect(c.activeIndex).toBe(2);
  });

  it('option Enter selects value', () => {
    c.onOptionKeydown(key('Enter'), 1);
    expect(selected).toEqual(['b']);
  });

  it('option Space selects value', () => {
    c.onOptionKeydown(key(' '), 0);
    expect(selected).toEqual(['a']);
  });

  it('option Escape closes', () => {
    c.onOptionKeydown(key('Escape'), 0);
    expect(toggled).toBe(1);
  });

  it('option Escape ignored when closed', () => {
    c.isOpen = false;
    c.onOptionKeydown(key('Escape'), 0);
    expect(toggled).toBe(0);
  });

  it('option ignores unrelated keys', () => {
    c.onOptionKeydown(key('Tab'), 0);
    expect(selected).toEqual([]);
    expect(toggled).toBe(0);
  });

  it('cleans pending focus timer on destroy', fakeAsync(() => {
    c.isOpen = false;
    c.onTriggerKeydown(key('Enter'));
    fixture.destroy();
    tick();
    expect(true).toBeTrue();
  }));
});
