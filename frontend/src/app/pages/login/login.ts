import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { AutofocusDirective } from '../../shared/directives/autofocus.directive';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { sanitizeFormValues } from '../../shared/utils/sanitize.utils';
import { extractErrorMessage } from '../../shared/utils/http-error.utils';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Banner, FormField, AutofocusDirective],
  templateUrl: './login.html',
})
export class Login {
  private static readonly REMEMBER_EMAIL_KEY = 'remember_email';

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
    rememberMe: [false]
  });

  loading = false;
  errorMessage = '';
  submitted = false;

  constructor() {
    const remembered = this.readRememberedEmail();
    if (remembered) {
      this.form.patchValue({ email: remembered, rememberMe: true });
    }
  }

  // tells the autofocus directive which input should receive focus on load
  get focusEmail(): boolean {
    return !this.form.value.email;
  }

  get focusPassword(): boolean {
    return !!this.form.value.email;
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';

      const { rememberMe, ...rawCredentials } = this.form.value;
      const credentials = sanitizeFormValues(rawCredentials);
      this.persistRememberedEmail(rememberMe, credentials.email);
      this.authService.login(credentials).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = extractErrorMessage(err, 'Credenciales incorrectas. Inténtalo de nuevo.');
        }
      });
    }
  }

  close(): void {
    this.router.navigate(['/']);
  }

  onOverlayClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.classList.contains('login-overlay')) {
      this.close();
    }
  }

  hasError(field: string): boolean {
    return hasFieldError(this.form, field, this.submitted);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  private readRememberedEmail(): string | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    try {
      return localStorage.getItem(Login.REMEMBER_EMAIL_KEY);
    } catch {
      // storage may be blocked in private mode
      return null;
    }
  }

  private persistRememberedEmail(remember: boolean, email: string): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      if (remember && email) {
        localStorage.setItem(Login.REMEMBER_EMAIL_KEY, email);
      } else {
        localStorage.removeItem(Login.REMEMBER_EMAIL_KEY);
      }
    } catch {
      // storage may be blocked in private mode
    }
  }
}
