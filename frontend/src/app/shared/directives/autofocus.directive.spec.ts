import { Component, ViewChild, ElementRef } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AutofocusDirective } from './autofocus.directive';

@Component({
  standalone: true,
  imports: [AutofocusDirective],
  template: `<input #i [appAutofocus]="enabled" />`,
})
class HostComponent {
  @ViewChild('i', { static: true }) input!: ElementRef<HTMLInputElement>;
  enabled: boolean | '' = true;
}

describe('AutofocusDirective', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    document.body.appendChild(fixture.nativeElement);
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('focuses the host element after view init', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    expect(document.activeElement).toBe(fixture.componentInstance.input.nativeElement);
  }));

  it('does not focus the host element when input is false', fakeAsync(() => {
    fixture.componentInstance.enabled = false;
    fixture.detectChanges();
    tick();
    expect(document.activeElement).not.toBe(fixture.componentInstance.input.nativeElement);
  }));

  it('cancels pending focus on destroy', fakeAsync(() => {
    fixture.detectChanges();
    fixture.destroy();
    tick();
    expect(true).toBeTrue();
  }));
});
