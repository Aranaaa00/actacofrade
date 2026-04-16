import { Component, inject, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, ValidationErrors, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { passwordStrength } from '../../shared/validators/password-strength.validator';
import { sanitizeFormValues } from '../../shared/utils/sanitize.utils';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, Banner, FormField],
  templateUrl: './register.html',
})
export class Register implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('registerModal') registerModal!: ElementRef<HTMLElement>;

  form: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150), Validators.pattern(/^[\p{L}\p{M} .'-]{3,150}$/u)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, passwordStrength]],
    confirmPassword: ['', [Validators.required]],
    roleCode: ['COLABORADOR', [Validators.required]],
    hermandadNombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200), Validators.pattern(/^[\p{L}\p{M}0-9 .,'()\-]{3,200}$/u)]]
  });

  loading = false;
  errorMessage = '';
  submitted = false;

  readonly roles = [
    { code: 'COLABORADOR', label: 'Colaborador' },
    { code: 'RESPONSABLE', label: 'Responsable' },
    { code: 'ADMINISTRADOR', label: 'Administrador' },
    { code: 'CONSULTA', label: 'Consulta' }
  ];

  get isAdmin(): boolean {
    return this.form.get('roleCode')?.value === 'ADMINISTRADOR';
  }

  get hermandadLabel(): string {
    return this.isAdmin ? 'Nombre de tu hermandad (nueva)' : 'Hermandad a la que perteneces';
  }

  get hermandadPlaceholder(): string {
    return this.isAdmin ? 'Hermandad de la Macarena...' : 'Nombre exacto de tu hermandad';
  }

  ngAfterViewInit(): void {
    const firstInput = this.registerModal.nativeElement.querySelector<HTMLInputElement>('#register-name');
    firstInput?.focus();
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid || !this.passwordsMatch()) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';

      const { confirmPassword, ...rawRequest } = this.form.value;
      const request = sanitizeFormValues({
        ...rawRequest,
        hermandadNombre: rawRequest.hermandadNombre.trim()
      });

      this.authService.register(request).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'No se pudo crear la cuenta. Inténtalo de nuevo.';
          this.loading = false;
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

  passwordsMatch(): boolean {
    return this.form.get('password')?.value === this.form.get('confirmPassword')?.value;
  }

  hasError(field: string): boolean {
    let showError = hasFieldError(this.form, field, this.submitted);
    if (field === 'confirmPassword' && this.submitted && !this.passwordsMatch()) {
      showError = true;
    }
    return showError;
  }

  getError(field: string): string {
    let message = getFieldError(this.form, field);
    if (!message) {
      const control = this.form.get(field);
      if (field === 'password' && control?.errors) {
        message = this.getPasswordError(control.errors);
      }
      if (field === 'confirmPassword' && this.submitted && !this.passwordsMatch()) {
        message = 'Las contraseñas no coinciden.';
      }
    }
    return message;
  }

  private getPasswordError(errors: ValidationErrors): string {
    const missing: string[] = [];
    if (errors['tooShort']) {
      missing.push('mínimo 8 caracteres');
    }
    if (errors['noUppercase']) {
      missing.push('una mayúscula');
    }
    if (errors['noLowercase']) {
      missing.push('una minúscula');
    }
    if (errors['noDigit']) {
      missing.push('un número');
    }
    if (errors['noSpecial']) {
      missing.push('un carácter especial (@$!%*?&.#_-)');
    }
    return 'Falta: ' + missing.join(', ') + '.';
  }
}
