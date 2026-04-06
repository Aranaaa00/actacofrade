import { Component, inject, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Banner, FormField],
  templateUrl: './login.html',
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
          this.router.navigate(['/dashboard']);
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
    return hasFieldError(this.form, field, this.submitted);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }
}
