import { AfterViewInit, Directive, ElementRef, Input, NgZone, inject } from '@angular/core';

// reusable directive that focuses its host element after the view is ready
// usage: <input appAutofocus> or <input [appAutofocus]="shouldFocus">
@Directive({
  selector: '[appAutofocus]',
})
export class AutofocusDirective implements AfterViewInit {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly zone = inject(NgZone);

  // when false the directive does nothing
  @Input() appAutofocus: boolean | '' = true;

  ngAfterViewInit(): void {
    if (this.appAutofocus === false) {
      return;
    }
    // defer outside Angular to avoid extra change detection cycles
    this.zone.runOutsideAngular(() => {
      setTimeout(() => this.host.nativeElement.focus());
    });
  }
}
