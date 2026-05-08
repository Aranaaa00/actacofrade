import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Banner } from './banner';

describe('Banner', () => {
  let fixture: ComponentFixture<Banner>;
  let banner: Banner;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Banner] }).compileComponents();
    fixture = TestBed.createComponent(Banner);
    banner = fixture.componentInstance;
  });

  it('defaults to status / polite for non-critical types', () => {
    banner.type = 'info';
    expect(banner.effectiveRole).toBe('status');
    expect(banner.effectiveAriaLive).toBe('polite');
  });

  it('uses alert / assertive for critical, danger and error', () => {
    banner.type = 'critical';
    expect(banner.effectiveRole).toBe('alert');
    expect(banner.effectiveAriaLive).toBe('assertive');
    banner.type = 'danger';
    expect(banner.effectiveRole).toBe('alert');
    banner.type = 'error';
    expect(banner.effectiveAriaLive).toBe('assertive');
  });

  it('honours explicit role and ariaLive overrides', () => {
    banner.type = 'critical';
    banner.role = 'note';
    banner.ariaLive = 'polite';
    expect(banner.effectiveRole).toBe('note');
    expect(banner.effectiveAriaLive).toBe('polite');
  });
});
