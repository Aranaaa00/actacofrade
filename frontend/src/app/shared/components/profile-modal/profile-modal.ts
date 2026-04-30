import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ProfileService } from '../../../services/profile.service';
import { AuthResponse } from '../../../models/auth.model';
import { passwordStrength } from '../../validators/password-strength.validator';
import { sanitizeText, noHtmlValidator } from '../../utils/sanitize.utils';

const NAME_PATTERN = /^[\p{L}\p{M} .'\-·]{3,150}$/u;

const ALLOWED_AVATAR_TYPES: ReadonlyArray<string> = [
  'image/png',
  'image/jpeg',
  'image/webp',
  'image/gif',
];

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

@Component({
  selector: 'app-profile-modal',
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './profile-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileModal implements OnChanges {
  @Input() show = false;
  @Input() user: AuthResponse | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() updated = new EventEmitter<AuthResponse>();

  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);

  readonly profileForm: FormGroup = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150), Validators.pattern(NAME_PATTERN), noHtmlValidator()]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
  });

  readonly passwordForm: FormGroup = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, passwordStrength]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [matchPasswords, differentFromCurrent] }
  );

  readonly profileSaving = signal(false);
  readonly passwordSaving = signal(false);
  readonly avatarSaving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly previewUrl = signal<string | null>(null);
  readonly storedAvatarUrl = signal<string | null>(null);
  readonly dragOver = signal(false);

  private pendingFile: File | null = null;

  readonly avatarSrc = computed(() => this.previewUrl() ?? this.storedAvatarUrl());

  readonly initials = computed(() => {
    const name = this.user?.fullName ?? '';
    return name.split(' ').filter(Boolean).slice(0, 2).map(n => n[0]?.toUpperCase()).join('');
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (this.show && this.user && (changes['show'] || changes['user'])) {
      this.resetForms();
      this.refreshStoredAvatar();
    }
    if (!this.show) {
      this.clearPreview();
      this.revokeStoredAvatar();
    }
  }

  onCancel(): void {
    this.closed.emit();
  }

  onSaveProfile(): void {
    // block submission while a previous save is still pending
    if (this.profileForm.invalid || this.profileSaving()) {
      this.profileForm.markAllAsTouched();
      return;
    }
    // strip html and normalise the email before sending it to the api
    const raw = this.profileForm.getRawValue() as { fullName: string; email: string };
    const payload = {
      fullName: sanitizeText(raw.fullName),
      email: sanitizeText(raw.email).toLowerCase(),
    };
    this.profileSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileService.updateProfile(payload).subscribe({
      next: (response) => {
        this.profileSaving.set(false);
        this.successMessage.set('Perfil actualizado correctamente');
        this.emitUpdate({ fullName: response.fullName, email: response.email, hasAvatar: response.hasAvatar });
      },
      error: (err) => {
        this.profileSaving.set(false);
        this.errorMessage.set(extractError(err));
      },
    });
  }

  onSavePassword(): void {
    if (this.passwordForm.invalid || this.passwordSaving()) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const raw = this.passwordForm.getRawValue() as { currentPassword: string; newPassword: string };
    this.passwordSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileService.changePassword({
      currentPassword: raw.currentPassword,
      newPassword: raw.newPassword,
    }).subscribe({
      next: () => {
        this.passwordSaving.set(false);
        this.successMessage.set('Contraseña actualizada correctamente');
        this.passwordForm.reset();
      },
      error: (err) => {
        this.passwordSaving.set(false);
        this.errorMessage.set(extractError(err));
      },
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  onFileSelected(event: Event): void {
    // reset the input value so the same file can be picked again later
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.handleFile(file);
    }
    input.value = '';
  }

  onConfirmAvatar(): void {
    // upload the staged file using multipart form data instead of json
    if (!this.pendingFile || this.avatarSaving()) {
      return;
    }
    this.avatarSaving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileService.uploadAvatar(this.pendingFile).subscribe({
      next: (response) => {
        this.avatarSaving.set(false);
        this.successMessage.set('Foto de perfil actualizada');
        this.clearPreview();
        this.emitUpdate({ hasAvatar: response.hasAvatar });
        this.refreshStoredAvatar();
      },
      error: (err) => {
        this.avatarSaving.set(false);
        this.errorMessage.set(extractError(err));
      },
    });
  }

  onDiscardAvatar(): void {
    this.clearPreview();
  }

  onDeleteAvatar(): void {
    if (this.avatarSaving() || !this.user?.hasAvatar) {
      return;
    }
    this.avatarSaving.set(true);
    this.errorMessage.set('');
    this.profileService.deleteAvatar().subscribe({
      next: () => {
        this.avatarSaving.set(false);
        this.successMessage.set('Foto de perfil eliminada');
        this.revokeStoredAvatar();
        this.emitUpdate({ hasAvatar: false });
      },
      error: (err) => {
        this.avatarSaving.set(false);
        this.errorMessage.set(extractError(err));
      },
    });
  }

  controlInvalid(form: FormGroup, name: string): boolean {
    const c = form.get(name);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  controlError(form: FormGroup, name: string): string {
    const c = form.get(name);
    if (!c || !c.errors || (!c.touched && !c.dirty)) {
      return '';
    }
    const errors = c.errors;
    if (errors['required']) return 'Este campo es obligatorio';
    if (errors['email']) return 'Introduce un correo electrónico válido';
    if (errors['minlength']) return 'Demasiado corto';
    if (errors['maxlength']) return 'Demasiado largo';
    if (errors['pattern']) return 'Formato no válido';
    if (errors['noHtml']) return 'No se permiten caracteres HTML';
    if (errors['tooShort']) return 'Mínimo 8 caracteres';
    if (errors['tooLong']) return 'Máximo 100 caracteres';
    if (errors['noUppercase']) return 'Debe incluir una mayúscula';
    if (errors['noLowercase']) return 'Debe incluir una minúscula';
    if (errors['noDigit']) return 'Debe incluir un número';
    if (errors['noSpecial']) return 'Debe incluir un carácter especial';
    if (errors['invalidChars']) return 'Solo letras, dígitos y @$!%*?&.#_-';
    return 'Valor no válido';
  }

  formError(form: FormGroup, key: string): boolean {
    return !!form.errors && !!form.errors[key];
  }

  private handleFile(file: File): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      this.errorMessage.set('Tipo de archivo no permitido. Usa PNG, JPG, WEBP o GIF.');
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      this.errorMessage.set('La imagen supera el tamaño máximo (2 MB).');
      return;
    }
    this.clearPreview();
    this.pendingFile = file;
    this.previewUrl.set(URL.createObjectURL(file));
  }

  private clearPreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.previewUrl.set(null);
    this.pendingFile = null;
  }

  private resetForms(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileForm.reset({
      fullName: this.user?.fullName ?? '',
      email: this.user?.email ?? '',
    });
    this.passwordForm.reset();
  }

  private refreshStoredAvatar(): void {
    this.revokeStoredAvatar();
    const u = this.user;
    if (!u || !u.hasAvatar) {
      return;
    }
    this.profileService.loadAvatar(u.userId).subscribe({
      next: (url) => this.storedAvatarUrl.set(url),
      error: () => this.storedAvatarUrl.set(null),
    });
  }

  private revokeStoredAvatar(): void {
    const url = this.storedAvatarUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.storedAvatarUrl.set(null);
  }

  private emitUpdate(patch: Partial<AuthResponse>): void {
    if (!this.user) {
      return;
    }
    const merged: AuthResponse = { ...this.user, ...patch };
    this.user = merged;
    this.updated.emit(merged);
  }

  hasPendingFile(): boolean {
    return this.pendingFile !== null;
  }
}

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const newPassword = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return newPassword && confirm && newPassword !== confirm ? { passwordMismatch: true } : null;
}

function differentFromCurrent(group: AbstractControl): ValidationErrors | null {
  const current = group.get('currentPassword')?.value;
  const next = group.get('newPassword')?.value;
  return current && next && current === next ? { sameAsCurrent: true } : null;
}

function extractError(err: unknown): string {
  const e = err as { error?: { message?: string } };
  return e?.error?.message ?? 'No se pudo completar la operación. Inténtalo de nuevo.';
}
