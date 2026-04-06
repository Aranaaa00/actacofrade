import { Component, inject, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

function passwordStrength(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value || '';
  const errors: ValidationErrors = {};

  if (value.length < 8) {
    errors['minlength'] = true;
  }
  if (!/[A-Z]/.test(value)) {
    errors['noUppercase'] = true;
  }
  if (!/[a-z]/.test(value)) {
    errors['noLowercase'] = true;
  }
  if (!/\d/.test(value)) {
    errors['noDigit'] = true;
  }
  if (!/[@$!%*?&.#_\-]/.test(value)) {
    errors['noSpecial'] = true;
  }

  return Object.keys(errors).length ? errors : null;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('registerModal') registerModal!: ElementRef<HTMLElement>;

  form: FormGroup = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordStrength]],
    confirmPassword: ['', [Validators.required]],
    roleCode: ['COLABORADOR', [Validators.required]]
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

      const { confirmPassword, ...request } = this.form.value;

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
    const control = this.form.get(field);
    let showError = false;
    if (control && control.invalid) {
      const isRequired = !!control.errors?.['required'];
      if (isRequired) {
        showError = this.submitted;
      } else {
        showError = control.touched;
      }
    }
    if (field === 'confirmPassword' && this.submitted && !this.passwordsMatch()) {
      showError = true;
    }
    return showError;
  }

  getError(field: string): string {
    const control = this.form.get(field);
    let message = '';
    if (control?.errors) {
      if (control.errors['required']) {
        message = 'Este campo es obligatorio.';
      } else if (control.errors['email']) {
        message = 'Introduce un email válido.';
      } else if (field === 'password') {
        message = this.getPasswordError(control.errors);
      }
    }
    if (!message && field === 'confirmPassword' && this.submitted && !this.passwordsMatch()) {
      message = 'Las contraseñas no coinciden.';
    }
    return message;
  }

  private getPasswordError(errors: ValidationErrors): string {
    const missing: string[] = [];
    if (errors['minlength']) {
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
