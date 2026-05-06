import { FormGroup } from '@angular/forms';

// True when the field should display its validation error in the UI.
// Errors are revealed as soon as the control is touched/dirty (real-time UX),
// or unconditionally after a submit attempt.
export function hasFieldError(form: FormGroup, field: string, submitted?: boolean): boolean {
  const control = form.get(field);
  let showError = false;
  if (control && control.invalid) {
    showError = !!submitted || control.dirty || control.touched;
  }
  return showError;
}

// Maps validator codes to a human-friendly Spanish error message.
export function getFieldError(form: FormGroup, field: string): string {
  const control = form.get(field);
  let message = '';
  if (control?.errors) {
    if (control.errors['required']) {
      message = 'Este campo es obligatorio.';
    } else if (control.errors['email']) {
      message = 'Introduce un email válido.';
    } else if (control.errors['minlength']) {
      message = `Mínimo ${control.errors['minlength'].requiredLength} caracteres.`;
    } else if (control.errors['maxlength']) {
      message = `Máximo ${control.errors['maxlength'].requiredLength} caracteres.`;
    } else if (control.errors['noHtml']) {
      message = 'No se permiten caracteres < o > en este campo.';
    } else if (control.errors['pattern']) {
      message = 'El valor introducido no tiene un formato válido.';
    }
  }
  return message;
}
