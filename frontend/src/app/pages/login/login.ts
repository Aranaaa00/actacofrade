import { Component, inject, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('loginModal') loginModal!: ElementRef<HTMLElement>;

  form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [false]
  });

  loading = false;
  errorMessage = '';
  submitted = false;

  ngAfterViewInit(): void {
    const firstInput = this.loginModal.nativeElement.querySelector<HTMLInputElement>('#login-email');
    firstInput?.focus();
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';

      this.authService.login(this.form.value).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/events/new']);
        },
        error: () => {
          this.errorMessage = 'Credenciales incorrectas. Inténtalo de nuevo.';
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

  hasError(field: string): boolean {
    const control = this.form.get(field);
    let showError = false;
    if (control && control.invalid) {
      const isRequired = !!control.errors?.['required'];
      if (isRequired) {
        showError = this.submitted;
      } else {
        showError = control.dirty;
      }
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
      }
    }
    return message;
  }
}
