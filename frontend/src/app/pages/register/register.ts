import { Component, inject, ElementRef, ViewChild, AfterViewInit, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/user.model';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { passwordStrength } from '../../shared/validators/password-strength.validator';
import { sanitizeFormValues } from '../../shared/utils/sanitize.utils';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, NgTemplateOutlet, Banner, FormField, ModalOverlay],
  templateUrl: './register.html',
})
export class Register implements OnInit, AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  @ViewChild('registerModal') registerModal?: ElementRef<HTMLElement>;

  @Input() embedded = false;
  @Output() userCreated = new EventEmitter<UserResponse>();
  @Output() dialogClosed = new EventEmitter<void>();

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

  get availableRoles() {
    return this.embedded ? this.roles.filter(r => r.code !== 'ADMINISTRADOR') : this.roles;
  }

  get bodyClass(): string {
    return this.embedded ? 'act-editor__body' : 'login__form';
  }

  get rowClass(): string {
    return this.embedded ? 'act-editor__row' : 'register__row';
  }

  get formAriaLabel(): string {
    return this.embedded ? 'Formulario de creación de usuario' : 'Formulario de registro';
  }

  ngOnInit(): void {
    if (this.embedded) {
      const hermandadControl = this.form.get('hermandadNombre');
      hermandadControl?.clearValidators();
      hermandadControl?.updateValueAndValidity();
    }
  }

  ngAfterViewInit(): void {
    const firstInput = this.registerModal?.nativeElement.querySelector<HTMLInputElement>('#register-name');
    firstInput?.focus();
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid || !this.passwordsMatch()) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';

      const { confirmPassword, hermandadNombre, ...rest } = this.form.value;

      if (this.embedded) {
        const request = sanitizeFormValues(rest);
        this.userService.create(request).subscribe({
          next: (created) => {
            this.loading = false;
            this.userCreated.emit(created);
          },
          error: (err) => {
            this.errorMessage = err.error?.message || 'No se pudo crear el usuario. Inténtalo de nuevo.';
            this.loading = false;
          }
        });
      } else {
        const request = sanitizeFormValues({
          ...rest,
          hermandadNombre: hermandadNombre.trim()
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
  }

  close(): void {
    if (this.embedded) {
      this.dialogClosed.emit();
    } else {
      this.router.navigate(['/']);
    }
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
