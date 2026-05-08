import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Tabs } from './tabs';

describe('Tabs', () => {
  let fixture: ComponentFixture<Tabs>;
  let tabs: Tabs;
  let emitted: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Tabs] }).compileComponents();
    fixture = TestBed.createComponent(Tabs);
    tabs = fixture.componentInstance;
    tabs.tabs = ['one', 'two', 'three'];
    tabs.activeTab = 'one';
    emitted = [];
    tabs.tabChanged.subscribe((t) => emitted.push(t));
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);
  });

  afterEach(() => fixture.destroy());

  it('emits selected tab on click', () => {
    tabs.selectTab('two');
    expect(emitted).toEqual(['two']);
  });

  function buildKeyEvent(key: string, index: number): KeyboardEvent {
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button.tab');
    const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true });
    Object.defineProperty(event, 'currentTarget', { value: buttons[index] });
    return event;
  }

  it('navigates with ArrowRight (wraps)', () => {
    tabs.onKeydown(buildKeyEvent('ArrowRight', 2), 2);
    expect(emitted).toEqual(['one']);
  });

  it('navigates with ArrowLeft (wraps)', () => {
    tabs.onKeydown(buildKeyEvent('ArrowLeft', 0), 0);
    expect(emitted).toEqual(['three']);
  });

  it('Home jumps to first tab', () => {
    tabs.onKeydown(buildKeyEvent('Home', 2), 2);
    expect(emitted).toEqual(['one']);
  });

  it('End jumps to last tab', () => {
    tabs.onKeydown(buildKeyEvent('End', 0), 0);
    expect(emitted).toEqual(['three']);
  });

  it('ignores irrelevant keys', () => {
    tabs.onKeydown(buildKeyEvent('Enter', 0), 0);
    expect(emitted).toEqual([]);
  });

  it('does nothing when there are no tabs', () => {
    tabs.tabs = [];
    tabs.onKeydown(buildKeyEvent('ArrowRight', 0), 0);
    expect(emitted).toEqual([]);
  });
});
