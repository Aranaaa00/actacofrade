import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { HermandadService } from '../../services/hermandad.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { HermandadResponse, HermandadUpdateRequest } from '../../models/hermandad.model';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { noHtmlValidator, sanitizeFormValues } from '../../shared/utils/sanitize.utils';
import { formatDateTime } from '../../shared/utils/date.utils';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, LucideAngularModule, Banner, FormField, ConfirmDialog],
  templateUrl: './settings.html',
})
export class Settings implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly hermandadService = inject(HermandadService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  form!: FormGroup;
  hermandad: HermandadResponse | null = null;
  loading = false;
  saving = false;
  deleting = false;
  showDeleteConfirm = false;
  hasLoadError = false;

  ngOnInit(): void {
    // build the brotherhood form with size and pattern validators that mirror the backend
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200), noHtmlValidator()]],
      descripcion: ['', [Validators.maxLength(500), noHtmlValidator()]],
      anioFundacion: [null, [Validators.min(1000), Validators.max(2100)]],
      localidad: ['', [Validators.maxLength(120), noHtmlValidator()]],
      direccionSede: ['', [Validators.maxLength(200), noHtmlValidator()]],
      emailContacto: ['', [Validators.email, Validators.maxLength(150)]],
      telefonoContacto: ['', [Validators.pattern(/^$|^[+0-9 ()-]{6,20}$/)]],
    });

    if (!this.canEdit) {
      // non admin users keep the form visible but in read only mode
      this.form.disable({ emitEvent: false });
    }

    this.load();
  }

  get canEdit(): boolean {
    return this.auth.isAdmin();
  }

  get formattedCreatedAt(): string {
    return this.hermandad ? formatDateTime(this.hermandad.createdAt) : '';
  }

  get formattedUpdatedAt(): string {
    return this.hermandad?.updatedAt ? formatDateTime(this.hermandad.updatedAt) : 'Sin cambios registrados';
  }

  get deleteConfirmMessage(): string {
    const name = this.hermandad?.nombre ?? '';
    return `¿Seguro que deseas eliminar "${name}"? Se borrarán todos los usuarios, actos y datos asociados de forma permanente.`;
  }

  hasError(field: string): boolean {
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  onSubmit(): void {
    // ignore re-submissions while a previous save is still pending
    if (!this.canEdit || this.saving) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warning('Revisa los campos marcados antes de guardar.');
      return;
    }

    this.saving = true;

    const sanitized = sanitizeFormValues(this.form.value) as HermandadUpdateRequest;
    this.hermandadService.updateCurrent(sanitized)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.applyResponse(response);
          this.form.markAsPristine();
          this.toast.success('Configuración guardada correctamente.');
          this.saving = false;
        },
        error: (err) => {
          this.toast.fromHttpError(err, 'No se pudo guardar la configuración. Inténtalo de nuevo.');
          this.saving = false;
        }
      });
  }

  onRequestDelete(): void {
    if (!this.canEdit || this.deleting) {
      return;
    }
    this.showDeleteConfirm = true;
  }

  onCancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  onConfirmDelete(): void {
    if (!this.canEdit || this.deleting) {
      return;
    }
    this.showDeleteConfirm = false;
    this.deleting = true;
    this.hermandadService.deleteCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          // session is invalid once the brotherhood disappears: drop credentials and go home
          this.auth.logout();
          this.toast.success('Hermandad eliminada correctamente.');
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.toast.fromHttpError(err, 'No se pudo eliminar la hermandad. Inténtalo de nuevo.');
          this.deleting = false;
        }
      });
  }

  private load(): void {
    this.loading = true;
    this.hasLoadError = false;
    this.hermandadService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.applyResponse(response);
          this.loading = false;
        },
        error: (err) => {
          this.hasLoadError = true;
          this.toast.fromHttpErrorSilencingAuth(err, 'No se pudo cargar la configuración de la hermandad.');
          this.loading = false;
        }
      });
  }

  private applyResponse(response: HermandadResponse): void {
    this.hermandad = response;
    this.form.patchValue({
      nombre: response.nombre,
      descripcion: response.descripcion ?? '',
      anioFundacion: response.anioFundacion,
      localidad: response.localidad ?? '',
      direccionSede: response.direccionSede ?? '',
      emailContacto: response.emailContacto ?? '',
      telefonoContacto: response.telefonoContacto ?? '',
    });
    this.form.markAsPristine();
    if (!this.canEdit) {
      this.form.disable({ emitEvent: false });
    }
  }
}
