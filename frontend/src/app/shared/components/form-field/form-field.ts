import {
  AfterContentInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  Renderer2,
  SimpleChanges,
} from '@angular/core';

@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.html',
})
export class FormField implements AfterContentInit, OnChanges {
  @Input() label = '';
  @Input() fieldId = '';
  @Input() hasError = false;
  @Input() errorMessage = '';
  @Input() styleClass = '';

  constructor(
    private readonly host: ElementRef<HTMLElement>,
    private readonly renderer: Renderer2,
  ) {}

  ngAfterContentInit(): void {
    this.syncControlAria();
  }

  ngOnChanges(_changes: SimpleChanges): void {
    this.syncControlAria();
  }

  get errorId(): string | null {
    return this.fieldId ? `${this.fieldId}-error` : null;
  }

  private syncControlAria(): void {
    const control = this.host.nativeElement.querySelector<HTMLElement>(
      'input, select, textarea',
    );
    if (!control) {
      return;
    }
    if (this.hasError) {
      this.renderer.setAttribute(control, 'aria-invalid', 'true');
    } else {
      this.renderer.removeAttribute(control, 'aria-invalid');
    }
    if (this.hasError && this.errorId) {
      this.renderer.setAttribute(control, 'aria-describedby', this.errorId);
    } else {
      this.renderer.removeAttribute(control, 'aria-describedby');
    }
  }
}
