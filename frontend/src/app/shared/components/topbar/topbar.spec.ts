import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Topbar } from './topbar';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

describe('Topbar', () => {
  let fixture: ComponentFixture<Topbar>;
  let t: Topbar;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Topbar], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(Topbar);
    t = fixture.componentInstance;
  });

  it('formattedDate returns a non-empty uppercase string', () => {
    expect(t.formattedDate.length).toBeGreaterThan(0);
    expect(t.formattedDate).toBe(t.formattedDate.toUpperCase());
  });

  it('todayIso returns YYYY-MM-DD', () => {
    expect(t.todayIso).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('emits toggledSidebar', () => {
    let n = 0;
    t.toggledSidebar.subscribe(() => n++);
    t.toggledSidebar.emit();
    expect(n).toBe(1);
  });
});
