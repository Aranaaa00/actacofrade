import { AfterViewInit, DestroyRef, Directive, ElementRef, Input, NgZone, inject } from '@angular/core';

// Reusable directive that focuses its host element on view init when appAutofocus is truthy.
@Directive({
  selector: '[appAutofocus]',
})
export class AutofocusDirective implements AfterViewInit {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  // When false the directive skips focusing the host element.
  @Input() appAutofocus: boolean | '' = true;

  ngAfterViewInit(): void {
    if (this.appAutofocus === false) {
      return;
    }
    // Defer focus outside Angular to avoid an extra change detection cycle.
    let timeoutId: ReturnType<typeof setTimeout> | undefined;
    this.zone.runOutsideAngular(() => {
      timeoutId = setTimeout(() => this.host.nativeElement.focus());
    });
    // Cancel any pending focus when the directive is destroyed.
    this.destroyRef.onDestroy(() => {
      if (timeoutId !== undefined) {
        clearTimeout(timeoutId);
      }
    });
  }
}
