import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { HermandadService } from '../../services/hermandad.service';
import { AuthService } from '../../services/auth.service';
import { HermandadResponse, HermandadUpdateRequest } from '../../models/hermandad.model';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { noHtmlValidator, sanitizeFormValues } from '../../shared/utils/sanitize.utils';
import { formatDateTime } from '../../shared/utils/date.utils';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, Banner, FormField],
  templateUrl: './settings.html',
})
export class Settings implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly hermandadService = inject(HermandadService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  hermandad: HermandadResponse | null = null;
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
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

  hasError(field: string): boolean {
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  onSubmit(): void {
    if (!this.canEdit || this.saving) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const sanitized = sanitizeFormValues(this.form.value) as HermandadUpdateRequest;
    this.hermandadService.updateCurrent(sanitized)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.applyResponse(response);
          this.form.markAsPristine();
          this.successMessage = 'Configuración guardada correctamente.';
          this.saving = false;
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'No se pudo guardar la configuración. Inténtalo de nuevo.';
          this.saving = false;
        }
      });
  }

  private load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.hermandadService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.applyResponse(response);
          this.loading = false;
        },
        error: () => {
          this.errorMessage = 'No se pudo cargar la configuración de la hermandad.';
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
