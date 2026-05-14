import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { FormField } from '../../../shared/components/form-field/form-field';

type State = 'form' | 'loading' | 'success' | 'invalid';

/**
 * Página pública para consumir el enlace de restablecimiento de contraseña
 * emitido por el SuperAdmin. El token llega en la query string y se envía al
 * backend junto con la nueva contraseña.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FormField],
  templateUrl: './reset-password.html',
})
export class ResetPassword implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  state = signal<State>('form');
  password = '';
  confirm = '';
  token = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('invalid');
      return;
    }
    this.token = token;
  }

  submit(form: NgForm): void {
    if (form.invalid || this.state() === 'loading') {
      return;
    }
    if (this.password !== this.confirm) {
      this.toast.error('Las contraseñas no coinciden.');
      return;
    }
    this.state.set('loading');
    this.authService
      .resetPassword(this.token, this.password)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.state.set('success');
          this.toast.success('Contraseña restablecida. Ya puedes iniciar sesión.');
          setTimeout(() => this.router.navigate(['/login']), 1500);
        },
        error: (err) => {
          this.state.set('form');
          this.toast.fromHttpError(err, 'El enlace no es válido o ha caducado.');
        },
      });
  }
}
