import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalOverlay } from './modal-overlay';
import { APP_ESCAPE_EVENT } from '../../services/browser.service';
import { LUCIDE_TEST_PROVIDERS } from '../../../../testing/lucide-test.providers';

@Component({
  standalone: true,
  imports: [ModalOverlay],
  template: `
    <app-modal-overlay [titleId]="'t'" (closed)="onClose()">
      <h2 modalTitle id="t">title</h2>
      <button>focusable</button>
    </app-modal-overlay>
  `,
})
class HostComponent {
  @ViewChild(ModalOverlay, { static: true }) overlay!: ModalOverlay;
  closes = 0;
  onClose() { this.closes++; }
}

describe('ModalOverlay', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent], providers: [LUCIDE_TEST_PROVIDERS] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('emits closed when overlay surface is clicked', () => {
    const overlayEl = fixture.nativeElement.querySelector('.modal-overlay');
    overlayEl.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    // The click event has target=section.modal-overlay so it should close
    fixture.detectChanges();
    expect(fixture.componentInstance.closes).toBeGreaterThan(0);
  });

  it('does not emit when click happens inside an inner element', () => {
    const before = fixture.componentInstance.closes;
    const inner = fixture.nativeElement.querySelector('button');
    fixture.componentInstance.overlay.onOverlayClick({ target: inner } as unknown as MouseEvent);
    expect(fixture.componentInstance.closes).toBe(before);
  });

  it('listens to APP_ESCAPE_EVENT and emits closed', () => {
    const before = fixture.componentInstance.closes;
    document.dispatchEvent(new CustomEvent(APP_ESCAPE_EVENT));
    expect(fixture.componentInstance.closes).toBeGreaterThan(before);
  });
});
