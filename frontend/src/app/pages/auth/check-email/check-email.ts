import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Página informativa mostrada tras solicitar el registro. Permite al usuario
 * reenviar el correo de verificación si no le ha llegado. No revela si el
 * email existe o no: la respuesta del backend siempre es genérica.
 */
@Component({
  selector: 'app-check-email',
  imports: [RouterLink],
  templateUrl: './check-email.html'
})
export class CheckEmail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly email: string | null = this.route.snapshot.queryParamMap.get('email');
  resending = false;
  lastResendAt: number | null = null;

  resend(): void {
    if (!this.email) {
      this.toast.warning('Vuelve a iniciar el registro para recibir el correo de verificación.');
      this.router.navigate(['/register']);
      return;
    }
    // Light client-side throttle to discourage abuse (server also rate-limits).
    if (this.lastResendAt && Date.now() - this.lastResendAt < 30_000) {
      this.toast.info('Espera unos segundos antes de volver a reenviar el correo.');
      return;
    }
    this.resending = true;
    this.authService.resendVerification({ email: this.email }).subscribe({
      next: (response) => {
        this.resending = false;
        this.lastResendAt = Date.now();
        this.toast.success(response.message);
      },
      error: (err) => {
        this.resending = false;
        this.toast.fromHttpError(err, 'No se pudo reenviar el correo de verificación.');
      }
    });
  }

  close(): void {
    this.router.navigate(['/']);
  }
}
