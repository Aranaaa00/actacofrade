import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Badge } from './badge';

describe('Badge', () => {
  let fixture: ComponentFixture<Badge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Badge] }).compileComponents();
    fixture = TestBed.createComponent(Badge);
  });

  it('creates with default inputs', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.variant).toBe('');
    expect(fixture.componentInstance.styleClass).toBe('');
  });

  it('accepts custom variant and styleClass', () => {
    fixture.componentInstance.variant = 'success';
    fixture.componentInstance.styleClass = 'big';
    fixture.detectChanges();
    expect(fixture.componentInstance.variant).toBe('success');
    expect(fixture.componentInstance.styleClass).toBe('big');
  });
});
