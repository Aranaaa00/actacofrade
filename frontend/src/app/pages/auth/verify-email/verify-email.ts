import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

type State = 'verifying' | 'success' | 'error';

/**
 * Consume el token recibido por correo y, si es válido, abre la sesión
 * del nuevo usuario. Los errores se muestran de forma genérica para evitar
 * filtrar si el token es desconocido, ha caducado o pertenece a otro flujo.
 */
@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  templateUrl: './verify-email.html'
})
export class VerifyEmail implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  state: State = 'verifying';
  errorMessage = 'El enlace de verificación no es válido o ha caducado.';
  private redirectTimeoutId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state = 'error';
      return;
    }
    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.state = 'success';
        this.toast.success('Correo verificado. Bienvenido a ActaCofrade.');
        // small delay so the user reads the success message before redirecting
        this.redirectTimeoutId = setTimeout(() => this.router.navigate(['/dashboard']), 1200);
      },
      error: () => {
        this.state = 'error';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.redirectTimeoutId !== null) {
      clearTimeout(this.redirectTimeoutId);
      this.redirectTimeoutId = null;
    }
  }
}
