import { FormGroup } from '@angular/forms';

export function hasFieldError(form: FormGroup, field: string, submitted?: boolean): boolean {
  const control = form.get(field);
  let showError = false;
  if (control && control.invalid) {
    if (submitted !== undefined) {
      const isRequired = !!control.errors?.['required'];
      showError = isRequired ? submitted : (control.dirty || control.touched);
    } else {
      showError = control.touched;
    }
  }
  return showError;
}

export function getFieldError(form: FormGroup, field: string): string {
  const control = form.get(field);
  let message = '';
  if (control?.errors) {
    if (control.errors['required']) {
      message = 'Este campo es obligatorio.';
    } else if (control.errors['email']) {
      message = 'Introduce un email válido.';
    } else if (control.errors['maxlength']) {
      message = `Máximo ${control.errors['maxlength'].requiredLength} caracteres.`;
    }
  }
  return message;
}
