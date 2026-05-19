import { AfterViewInit, Directive, ElementRef, OnDestroy, Renderer2, inject } from '@angular/core';

const ICON_SHOW = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>';
const ICON_HIDE = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><line x1="2" x2="22" y1="2" y2="22"/></svg>';

@Directive({
  selector: 'input[appPasswordToggle]',
  standalone: true,
})
export class PasswordToggleDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject(ElementRef<HTMLInputElement>);
  private readonly renderer = inject(Renderer2);

  private wrapper: HTMLElement | null = null;
  private button: HTMLButtonElement | null = null;
  private visible = false;
  private clickListener: (() => void) | null = null;

  ngAfterViewInit(): void {
    const input = this.host.nativeElement;
    const parent = input.parentNode as HTMLElement | null;
    if (!parent) {
      return;
    }

    this.wrapper = this.renderer.createElement('span');
    this.renderer.addClass(this.wrapper, 'password-toggle');

    this.button = this.renderer.createElement('button');
    this.renderer.setAttribute(this.button, 'type', 'button');
    this.renderer.setAttribute(this.button, 'aria-label', 'Mostrar contraseña');
    this.renderer.setAttribute(this.button, 'aria-pressed', 'false');
    this.renderer.addClass(this.button, 'password-toggle__button');
    this.renderer.setProperty(this.button, 'innerHTML', ICON_SHOW);

    this.renderer.insertBefore(parent, this.wrapper, input);
    this.renderer.appendChild(this.wrapper, input);
    this.renderer.appendChild(this.wrapper, this.button);

    this.clickListener = this.renderer.listen(this.button, 'click', () => this.toggle());
  }

  ngOnDestroy(): void {
    if (this.clickListener) {
      this.clickListener();
      this.clickListener = null;
    }
  }

  private toggle(): void {
    this.visible = !this.visible;
    const input = this.host.nativeElement;
    this.renderer.setAttribute(input, 'type', this.visible ? 'text' : 'password');
    if (this.button) {
      this.renderer.setProperty(this.button, 'innerHTML', this.visible ? ICON_HIDE : ICON_SHOW);
      this.renderer.setAttribute(this.button, 'aria-label', this.visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
      this.renderer.setAttribute(this.button, 'aria-pressed', this.visible ? 'true' : 'false');
    }
  }
}
