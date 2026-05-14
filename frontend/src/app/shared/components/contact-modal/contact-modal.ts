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
import { ModalA11yDirective } from '../../directives/modal-a11y.directive';
import { SUPPORT_CATEGORIES, SupportCategory, SupportCategoryKey } from '../../constants/support.const';

// Centralized support hub: admin change, password reset, manual verification and SuperAdmin contact.
@Component({
  selector: 'app-contact-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, FormField, ModalA11yDirective],
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

  readonly categories = SUPPORT_CATEGORIES;

  form: FormGroup = this.fb.group({
    category: [SUPPORT_CATEGORIES[0].key as SupportCategoryKey, [Validators.required]],
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000), noHtmlValidator()]],
  });

  submitting = false;

  get hermandadName(): string {
    return this.auth.getUser()?.hermandadNombre ?? '';
  }

  get activeCategory(): SupportCategory {
    const key = this.form.controls['category'].value as SupportCategoryKey;
    return this.categories.find(c => c.key === key) ?? this.categories[0];
  }

  get remainingChars(): number {
    const value: string = this.form.controls['message'].value ?? '';
    return 2000 - value.length;
  }

  selectCategory(key: SupportCategoryKey): void {
    if (this.submitting) {
      return;
    }
    this.form.controls['category'].setValue(key);
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
    this.form.reset({ category: this.categories[0].key, message: '' });
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

    const raw = sanitizeFormValues(this.form.value) as { category: SupportCategoryKey; message: string };
    const category = this.categories.find(c => c.key === raw.category) ?? this.categories[0];
    const payload = { message: `${category.prefix} ${raw.message}` };
    this.requestService.create(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Solicitud enviada correctamente. El equipo la revisará pronto.');
          this.form.reset({ category: this.categories[0].key, message: '' });
          this.closed.emit();
        },
        error: (err) => {
          this.submitting = false;
          this.toast.fromHttpError(err, 'No se pudo enviar la solicitud. Inténtalo de nuevo.');
        },
      });
  }
}
