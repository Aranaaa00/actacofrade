import { Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { FormField } from '../form-field/form-field';
import { AdminChangeRequestService } from '../../../services/admin-change-request.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { hasFieldError, getFieldError } from '../../utils/form-validation.utils';
import { noHtmlValidator, sanitizeFormValues } from '../../utils/sanitize.utils';

// Reusable modal to send an admin change request from the sidebar.
@Component({
  selector: 'app-contact-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, FormField],
  templateUrl: './contact-modal.html',
})
export class ContactModal {
  private readonly fb = inject(FormBuilder);
  private readonly requestService = inject(AdminChangeRequestService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  @Input() show = false;
  @Output() closed = new EventEmitter<void>();

  form: FormGroup = this.fb.group({
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000), noHtmlValidator()]],
  });

  submitting = false;

  get hermandadName(): string {
    return this.auth.getUser()?.hermandadNombre ?? '';
  }

  get remainingChars(): number {
    const value: string = this.form.controls['message'].value ?? '';
    return 2000 - value.length;
  }

  hasError(field: string): boolean {
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  onClose(): void {
    if (this.submitting) {
      return;
    }
    this.form.reset({ message: '' });
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.submitting) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warning('Revisa los campos marcados antes de enviar.');
      return;
    }

    this.submitting = true;

    const payload = sanitizeFormValues(this.form.value) as { message: string };
    this.requestService.create(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Solicitud enviada correctamente. El equipo la revisará pronto.');
          this.form.reset({ message: '' });
          this.closed.emit();
        },
        error: (err) => {
          this.submitting = false;
          this.toast.fromHttpError(err, 'No se pudo enviar la solicitud. Inténtalo de nuevo.');
        },
      });
  }
}
